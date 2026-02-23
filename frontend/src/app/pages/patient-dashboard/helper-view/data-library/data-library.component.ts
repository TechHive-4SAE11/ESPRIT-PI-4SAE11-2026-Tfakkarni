import {
  Component, Input, Output, EventEmitter, signal, inject, DestroyRef, OnInit,
  AfterViewInit, OnDestroy, ViewChild, ElementRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, of, tap, finalize, Subject, debounceTime, switchMap } from 'rxjs';

import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardBadgeComponent } from '@/shared/components/badge';

import { DataPointService, type DataPointSummary, type DataPointType, type UpdateDataPointRequest } from '@/core/services/data-point.service';
import { MemoryTagService, type TagResponse } from '@/core/services/memory-tag.service';
import { MovieGameService, type TmdbMovie } from '@/core/services/movie-game.service';
import { photoSchema, placeSchema, movieMemorySchema, questionMemorySchema, getFieldErrors } from '@/core/validation/game-schemas';
import * as L from 'leaflet';

type View = 'list' | 'add-photo' | 'add-place' | 'add-movie' | 'add-question' | 'edit';

@Component({
  selector: 'app-data-library',
  standalone: true,
  imports: [CommonModule, FormsModule, ZardCardComponent, ZardIconComponent, ZardButtonComponent, ZardBadgeComponent],
  template: `
    <div class="space-y-6">
      <!-- Header -->
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-2xl font-bold text-foreground">Data Library</h2>
          <p class="text-muted-foreground">Upload photos, places, movies & questions</p>
        </div>
        <z-button variant="outline" (click)="goBack.emit()">
          <z-icon zType="arrow-left" class="mr-2 h-4 w-4" /> Back
        </z-button>
      </div>

      @switch (view()) {
        @case ('list') {
          <!-- Type Filters -->
          <div class="flex flex-wrap gap-2">
            <button (click)="toggleTypeFilter(null)"
              class="px-3 py-1.5 rounded-full text-sm font-medium transition-colors"
              [class]="!activeTypeFilter() ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground hover:bg-muted/80'">
              All
            </button>
            @for (t of typeFilters; track t.type) {
              <button (click)="toggleTypeFilter(t.type)"
                class="px-3 py-1.5 rounded-full text-sm font-medium transition-colors"
                [class]="activeTypeFilter() === t.type ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground hover:bg-muted/80'">
                {{ t.emoji }} {{ t.label }}
              </button>
            }
          </div>

          <!-- Tag Filters -->
          @if (tags().length > 0) {
            <div class="flex flex-wrap gap-1.5">
              @for (tag of tags(); track tag.id) {
                <button (click)="toggleTagFilter(tag.id)"
                  class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border transition-colors"
                  [class]="activeTagIds().has(tag.id) ? 'border-primary bg-primary/10 text-primary' : 'border-border hover:border-primary/50'">
                  <span class="w-2 h-2 rounded-full" [style.background-color]="tag.color"></span>
                  {{ tag.name }}
                </button>
              }
            </div>
          }

          <!-- Add Buttons -->
          <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <z-button variant="outline" class="h-auto py-3" (click)="view.set('add-photo')">
              <div class="flex flex-col items-center gap-1">
                <span class="text-xl">📷</span>
                <span class="text-xs">Add Photo</span>
              </div>
            </z-button>
            <z-button variant="outline" class="h-auto py-3" (click)="view.set('add-place'); initPlaceMap()">
              <div class="flex flex-col items-center gap-1">
                <span class="text-xl">📍</span>
                <span class="text-xs">Add Place</span>
              </div>
            </z-button>
            <z-button variant="outline" class="h-auto py-3" (click)="view.set('add-movie')">
              <div class="flex flex-col items-center gap-1">
                <span class="text-xl">🎬</span>
                <span class="text-xs">Add Movie</span>
              </div>
            </z-button>
            <z-button variant="outline" class="h-auto py-3" (click)="view.set('add-question')">
              <div class="flex flex-col items-center gap-1">
                <span class="text-xl">❓</span>
                <span class="text-xs">Add Question</span>
              </div>
            </z-button>
          </div>

          <!-- Data Point Grid -->
          @if (isLoading()) {
            <div class="text-center py-12 text-muted-foreground">Loading data points...</div>
          } @else if (filteredItems().length === 0) {
            <div class="text-center py-12">
              <p class="text-4xl mb-3">📂</p>
              <p class="text-lg font-semibold text-muted-foreground">No data points yet</p>
              <p class="text-sm text-muted-foreground">Upload photos, add places, movies, or questions above</p>
            </div>
          } @else {
            <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              @for (item of filteredItems(); track item.id + '-' + item.type) {
                <z-card class="p-4 relative group">
                  <!-- Type badge -->
                  <div class="flex items-center justify-between mb-2">
                    <span class="text-xs font-medium px-2 py-0.5 rounded-full"
                      [class]="getTypeBadgeClass(item.type)">
                      {{ getTypeEmoji(item.type) }} {{ item.type }}
                    </span>
                    <button (click)="openEdit(item)"
                      class="opacity-0 group-hover:opacity-100 text-muted-foreground hover:text-primary transition-all">
                      <z-icon zType="edit" class="h-4 w-4" />
                    </button>
                    <button (click)="deleteItem(item)"
                      class="opacity-0 group-hover:opacity-100 text-muted-foreground hover:text-red-500 transition-all">
                      <z-icon zType="trash-2" class="h-4 w-4" />
                    </button>
                  </div>
                  <!-- Preview -->
                  @if (item.type === 'PHOTO' && item.imagePreview) {
                    <img [src]="'data:image/jpeg;base64,' + item.imagePreview"
                      class="w-full h-24 object-cover rounded-md mb-2" />
                  }
                  @if (item.type === 'MOVIE' && item.posterPath) {
                    <img [src]="'https://image.tmdb.org/t/p/w200' + item.posterPath"
                      class="w-full h-24 object-cover rounded-md mb-2" />
                  }
                  <!-- Label -->
                  <p class="font-semibold text-sm truncate">{{ item.label }}</p>
                  <p class="text-xs text-muted-foreground truncate">{{ item.subtitle }}</p>
                  @if (item.correctAnswer) {
                    <p class="text-xs text-primary mt-1 truncate">🎯 Answer: {{ item.correctAnswer }}</p>
                  }
                  <!-- Tags -->
                  @if (item.tags && item.tags.length > 0) {
                    <div class="flex flex-wrap gap-1 mt-2">
                      @for (tag of item.tags; track tag.id) {
                        <span class="inline-flex items-center gap-1 px-1.5 py-0.5 rounded-full text-[10px] border border-border">
                          <span class="w-1.5 h-1.5 rounded-full" [style.background-color]="tag.color"></span>
                          {{ tag.name }}
                        </span>
                      }
                    </div>
                  }
                </z-card>
              }
            </div>
          }
        }

        <!-- ════════ ADD PHOTO ════════ -->
        @case ('add-photo') {
          <z-card class="p-6">
            <div class="flex items-center gap-2 mb-4">
              <button (click)="view.set('list')" class="text-muted-foreground hover:text-foreground">
                <z-icon zType="arrow-left" class="h-5 w-5" />
              </button>
              <h3 class="text-lg font-semibold">📷 Add Photo</h3>
            </div>
            <div class="space-y-4">
              <div>
                <label class="text-sm font-medium block mb-1">Name / Label <span class="text-muted-foreground font-normal">(max 20)</span></label>
                <input type="text" [(ngModel)]="photoName" placeholder="e.g., Grandma Sara" maxlength="20"
                  class="w-full rounded-md border bg-background px-3 py-2 text-sm"
                  [class]="formErrors()['name'] ? 'border-red-500' : 'border-border'" />
                @if (formErrors()['name']) {
                  <p class="text-xs text-red-500 mt-1">{{ formErrors()['name'] }}</p>
                }
              </div>
              <div>
                <label class="text-sm font-medium block mb-1">Image <span class="text-muted-foreground font-normal">(max 5MB)</span></label>
                <input type="file" accept="image/*" (change)="onPhotoSelected($event)"
                  class="w-full text-sm file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-medium file:bg-primary file:text-primary-foreground hover:file:bg-primary/90" />
                @if (formErrors()['imageBase64']) {
                  <p class="text-xs text-red-500 mt-1">{{ formErrors()['imageBase64'] }}</p>
                }
                @if (photoPreview()) {
                  <img [src]="photoPreview()" class="mt-2 max-h-40 rounded-md" />
                }
              </div>
              <div>
                <label class="text-sm font-medium block mb-1">Tags</label>
                <div class="flex flex-wrap gap-1.5">
                  @for (tag of tags(); track tag.id) {
                    <button (click)="toggleSelectedTag(tag.id)"
                      class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium border transition-colors"
                      [class]="selectedTagIds().has(tag.id) ? 'border-primary bg-primary/10 text-primary' : 'border-border'">
                      <span class="w-2 h-2 rounded-full" [style.background-color]="tag.color"></span>
                      {{ tag.name }}
                    </button>
                  }
                </div>
              </div>
              <button z-button (click)="submitPhoto()" [disabled]="!photoName.trim() || !photoBase64() || isSaving()">
                @if (isSaving()) { Saving... } @else { <z-icon zType="upload" class="mr-2 h-4 w-4" /> Save Photo }
              </button>
            </div>
          </z-card>
        }

        <!-- ════════ ADD PLACE ════════ -->
        @case ('add-place') {
          <z-card class="p-6">
            <div class="flex items-center gap-2 mb-4">
              <button (click)="view.set('list'); destroyPlaceMap()" class="text-muted-foreground hover:text-foreground">
                <z-icon zType="arrow-left" class="h-5 w-5" />
              </button>
              <h3 class="text-lg font-semibold">📍 Add Place</h3>
            </div>
            <p class="text-sm text-muted-foreground mb-3">
              <z-icon zType="map-pin" class="inline mr-1" />
              Click on the map to pin a location
            </p>
            <!-- Leaflet Map -->
            <div
              #placeMapContainer
              class="w-full h-[350px] rounded-lg border border-border mb-4 z-0"
            ></div>
            @if (placeLat !== null && placeLng !== null) {
              <div class="flex gap-3 mb-4">
                <span class="text-xs px-2.5 py-1 rounded-full bg-muted text-muted-foreground">
                  Lat: {{ placeLat!.toFixed(5) }}
                </span>
                <span class="text-xs px-2.5 py-1 rounded-full bg-muted text-muted-foreground">
                  Lng: {{ placeLng!.toFixed(5) }}
                </span>
              </div>
            }
            <div class="space-y-4">
              <div>
                <label class="text-sm font-medium block mb-1">Place Name <span class="text-muted-foreground font-normal">(max 20)</span></label>
                <input type="text" [(ngModel)]="placeName" placeholder="e.g., Childhood Home" maxlength="20"
                  class="w-full rounded-md border bg-background px-3 py-2 text-sm"
                  [class]="formErrors()['name'] ? 'border-red-500' : 'border-border'" />
                @if (formErrors()['name']) {
                  <p class="text-xs text-red-500 mt-1">{{ formErrors()['name'] }}</p>
                }
              </div>
              <div>
                <label class="text-sm font-medium block mb-1">Hint (optional) <span class="text-muted-foreground font-normal">(max 100)</span></label>
                <input type="text" [(ngModel)]="placeHint" placeholder="A clue about this place..." maxlength="100"
                  class="w-full rounded-md border bg-background px-3 py-2 text-sm"
                  [class]="formErrors()['hint'] ? 'border-red-500' : 'border-border'" />
                @if (formErrors()['hint']) {
                  <p class="text-xs text-red-500 mt-1">{{ formErrors()['hint'] }}</p>
                }
              </div>
              <div>
                <label class="text-sm font-medium block mb-1">Tags</label>
                <div class="flex flex-wrap gap-1.5">
                  @for (tag of tags(); track tag.id) {
                    <button (click)="toggleSelectedTag(tag.id)"
                      class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium border transition-colors"
                      [class]="selectedTagIds().has(tag.id) ? 'border-primary bg-primary/10 text-primary' : 'border-border'">
                      <span class="w-2 h-2 rounded-full" [style.background-color]="tag.color"></span>
                      {{ tag.name }}
                    </button>
                  }
                </div>
              </div>
              <button z-button (click)="submitPlace()" [disabled]="!placeName.trim() || placeLat === null || placeLng === null || isSaving()">
                @if (isSaving()) { Saving... } @else { <z-icon zType="map-pin" class="mr-2 h-4 w-4" /> Save Place }
              </button>
            </div>
          </z-card>
        }

        <!-- ════════ ADD MOVIE ════════ -->
        @case ('add-movie') {
          <z-card class="p-6">
            <div class="flex items-center gap-2 mb-4">
              <button (click)="view.set('list')" class="text-muted-foreground hover:text-foreground">
                <z-icon zType="arrow-left" class="h-5 w-5" />
              </button>
              <h3 class="text-lg font-semibold">🎬 Add Movie</h3>
            </div>
            <div class="space-y-4">
              <div>
                <label class="text-sm font-medium block mb-1">Search Movie</label>
                <input type="text" [(ngModel)]="movieSearchQuery" (ngModelChange)="onMovieSearch($event)"
                  placeholder="Search by title..."
                  class="w-full rounded-md border border-border bg-background px-3 py-2 text-sm" />
              </div>
              @if (movieSearchResults().length > 0 && !selectedMovie()) {
                <div class="max-h-60 overflow-y-auto border border-border rounded-md">
                  @for (movie of movieSearchResults(); track movie.id) {
                    <button (click)="selectMovie(movie)"
                      class="w-full flex items-center gap-3 p-3 text-left hover:bg-muted transition-colors border-b border-border last:border-b-0">
                      @if (movie.poster_path) {
                        <img [src]="'https://image.tmdb.org/t/p/w92' + movie.poster_path"
                          class="w-10 h-14 object-cover rounded" />
                      }
                      <div>
                        <p class="font-medium text-sm">{{ movie.title }}</p>
                        <p class="text-xs text-muted-foreground">{{ movie.release_date }}</p>
                      </div>
                    </button>
                  }
                </div>
              }
              @if (selectedMovie()) {
                <div class="flex items-center gap-3 p-3 bg-muted rounded-md">
                  @if (selectedMovie()!.poster_path) {
                    <img [src]="'https://image.tmdb.org/t/p/w92' + selectedMovie()!.poster_path"
                      class="w-10 h-14 object-cover rounded" />
                  }
                  <div class="flex-1">
                    <p class="font-medium text-sm">{{ selectedMovie()!.title }}</p>
                    <p class="text-xs text-muted-foreground">{{ selectedMovie()!.release_date }}</p>
                  </div>
                  <button (click)="clearMovie()" class="text-muted-foreground hover:text-foreground">
                    <z-icon zType="x" class="h-4 w-4" />
                  </button>
                </div>
                <div>
                  <label class="text-sm font-medium block mb-1">Character Name (correct answer) <span class="text-muted-foreground font-normal">(max 20)</span></label>
                  <input type="text" [(ngModel)]="movieCharacterName" placeholder="e.g., Jack Dawson" maxlength="20"
                    class="w-full rounded-md border bg-background px-3 py-2 text-sm"
                    [class]="formErrors()['correctAnswer'] ? 'border-red-500' : 'border-border'" />
                  @if (formErrors()['correctAnswer']) {
                    <p class="text-xs text-red-500 mt-1">{{ formErrors()['correctAnswer'] }}</p>
                  }
                </div>
              }
              <div>
                <label class="text-sm font-medium block mb-1">Tags</label>
                <div class="flex flex-wrap gap-1.5">
                  @for (tag of tags(); track tag.id) {
                    <button (click)="toggleSelectedTag(tag.id)"
                      class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium border transition-colors"
                      [class]="selectedTagIds().has(tag.id) ? 'border-primary bg-primary/10 text-primary' : 'border-border'">
                      <span class="w-2 h-2 rounded-full" [style.background-color]="tag.color"></span>
                      {{ tag.name }}
                    </button>
                  }
                </div>
              </div>
              <button z-button (click)="submitMovie()" [disabled]="!selectedMovie() || !movieCharacterName.trim() || isSaving()">
                @if (isSaving()) { Saving... } @else { <z-icon zType="save" class="mr-2 h-4 w-4" /> Save Movie }
              </button>
            </div>
          </z-card>
        }

        <!-- ════════ ADD QUESTION ════════ -->
        @case ('add-question') {
          <z-card class="p-6">
            <div class="flex items-center gap-2 mb-4">
              <button (click)="view.set('list')" class="text-muted-foreground hover:text-foreground">
                <z-icon zType="arrow-left" class="h-5 w-5" />
              </button>
              <h3 class="text-lg font-semibold">❓ Add Personal Question</h3>
            </div>
            <div class="space-y-4">
              <!-- Quick suggestions -->
              <div>
                <label class="text-sm font-medium block mb-1">Quick Templates</label>
                <div class="flex flex-wrap gap-1.5">
                  @for (s of questionSuggestions; track s) {
                    <button (click)="questionText = s"
                      class="text-xs px-2 py-1 rounded-md bg-muted hover:bg-muted/80 text-muted-foreground transition-colors">
                      {{ s }}
                    </button>
                  }
                </div>
              </div>
              <div>
                <label class="text-sm font-medium block mb-1">Question <span class="text-muted-foreground font-normal">(max 500)</span></label>
                <input type="text" [(ngModel)]="questionText" placeholder="e.g., Where were you born?"
                  class="w-full rounded-md border bg-background px-3 py-2 text-sm"
                  [class]="formErrors()['questionText'] ? 'border-red-500' : 'border-border'" />
                @if (formErrors()['questionText']) {
                  <p class="text-xs text-red-500 mt-1">{{ formErrors()['questionText'] }}</p>
                }
              </div>
              <div>
                <label class="text-sm font-medium block mb-1">Correct Answer <span class="text-muted-foreground font-normal">(max 500)</span></label>
                <input type="text" [(ngModel)]="questionAnswer" placeholder="e.g., Tunis"
                  class="w-full rounded-md border bg-background px-3 py-2 text-sm"
                  [class]="formErrors()['correctAnswer'] ? 'border-red-500' : 'border-border'" />
                @if (formErrors()['correctAnswer']) {
                  <p class="text-xs text-red-500 mt-1">{{ formErrors()['correctAnswer'] }}</p>
                }
              </div>
              <div>
                <label class="text-sm font-medium block mb-1">Tags</label>
                <div class="flex flex-wrap gap-1.5">
                  @for (tag of tags(); track tag.id) {
                    <button (click)="toggleSelectedTag(tag.id)"
                      class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium border transition-colors"
                      [class]="selectedTagIds().has(tag.id) ? 'border-primary bg-primary/10 text-primary' : 'border-border'">
                      <span class="w-2 h-2 rounded-full" [style.background-color]="tag.color"></span>
                      {{ tag.name }}
                    </button>
                  }
                </div>
              </div>
              <button z-button (click)="submitQuestion()" [disabled]="!questionText.trim() || !questionAnswer.trim() || isSaving()">
                @if (isSaving()) { Saving... } @else { <z-icon zType="save" class="mr-2 h-4 w-4" /> Save Question }
              </button>
            </div>
          </z-card>
        }

        <!-- ════════ EDIT DATA POINT ════════ -->
        @case ('edit') {
          @if (editingItem(); as ei) {
            <z-card class="p-6">
              <div class="flex items-center gap-2 mb-4">
                <button (click)="destroyEditMap(); view.set('list'); editingItem.set(null)" class="text-muted-foreground hover:text-foreground">
                  <z-icon zType="arrow-left" class="h-5 w-5" />
                </button>
                <h3 class="text-lg font-semibold">{{ getTypeEmoji(ei.type) }} Edit {{ ei.type | titlecase }}</h3>
              </div>

              <!-- Preview (read-only) -->
              @if (ei.type === 'PHOTO' && ei.imagePreview) {
                <img [src]="'data:image/jpeg;base64,' + ei.imagePreview"
                  class="w-full h-40 object-contain rounded-md mb-4 bg-muted" />
              }
              @if (ei.type === 'MOVIE' && ei.posterPath) {
                <img [src]="'https://image.tmdb.org/t/p/w200' + ei.posterPath"
                  class="w-full h-40 object-contain rounded-md mb-4 bg-muted" />
              }

              <div class="space-y-4">
                <!-- Name (PHOTO & PLACE) -->
                @if (ei.type === 'PHOTO' || ei.type === 'PLACE') {
                  <div>
                    <label class="text-sm font-medium block mb-1">Name / Label (this is the game answer)</label>
                    <input type="text" [(ngModel)]="editName"
                      class="w-full rounded-md border border-border bg-background px-3 py-2 text-sm" />
                  </div>
                }

                <!-- Hint (PLACE only) -->
                @if (ei.type === 'PLACE') {
                  <div>
                    <label class="text-sm font-medium block mb-1">Hint</label>
                    <input type="text" [(ngModel)]="editHint" placeholder="A clue about this place..."
                      class="w-full rounded-md border border-border bg-background px-3 py-2 text-sm" />
                  </div>

                  <!-- Map for editing pin location -->
                  <div>
                    <label class="text-sm font-medium block mb-1">📍 Pin Location (click to move)</label>
                    <div #editMapContainer class="w-full h-64 rounded-md border border-border z-0"></div>
                    @if (editLat != null && editLng != null) {
                      <p class="text-xs text-muted-foreground mt-1">📌 {{ editLat | number:'1.4-4' }}, {{ editLng | number:'1.4-4' }}</p>
                    }
                  </div>
                }

                <!-- Movie title (read-only) -->
                @if (ei.type === 'MOVIE') {
                  <div>
                    <label class="text-sm font-medium block mb-1">Movie</label>
                    <p class="text-sm font-semibold px-3 py-2 bg-muted rounded-md">{{ ei.label }}</p>
                  </div>
                }

                <!-- Question text (QUESTION) -->
                @if (ei.type === 'QUESTION') {
                  <div>
                    <label class="text-sm font-medium block mb-1">Question</label>
                    <input type="text" [(ngModel)]="editQuestionText"
                      class="w-full rounded-md border border-border bg-background px-3 py-2 text-sm" />
                  </div>
                }

                <!-- Correct Answer (MOVIE & QUESTION) -->
                @if (ei.type === 'MOVIE' || ei.type === 'QUESTION') {
                  <div>
                    <label class="text-sm font-medium block mb-1">
                      {{ ei.type === 'MOVIE' ? 'Character Name (game answer)' : 'Correct Answer' }}
                    </label>
                    <input type="text" [(ngModel)]="editCorrectAnswer"
                      class="w-full rounded-md border border-border bg-background px-3 py-2 text-sm"
                      [class.border-red-500]="!editCorrectAnswer.trim()"
                      [placeholder]="ei.type === 'MOVIE' ? 'e.g., Jack Dawson' : 'e.g., Tunis'" />
                    @if (!editCorrectAnswer.trim()) {
                      <p class="text-xs text-red-500 mt-1">⚠️ Answer is required — this is what the patient must guess!</p>
                    }
                  </div>
                }

                <button z-button (click)="submitEdit()" [disabled]="isEditInvalid() || isSaving()">
                  @if (isSaving()) { Saving... } @else { <z-icon zType="save" class="mr-2 h-4 w-4" /> Save Changes }
                </button>
              </div>
            </z-card>
          }
        }
      }
    </div>
  `,
})
export class DataLibraryComponent implements OnInit, OnDestroy {
  @Input({ required: true }) keycloakId!: string;
  @Output() goBack = new EventEmitter<void>();
  @ViewChild('placeMapContainer') placeMapContainer!: ElementRef;
  @ViewChild('editMapContainer') editMapContainer!: ElementRef;

  private readonly dataPointService = inject(DataPointService);
  private readonly tagService = inject(MemoryTagService);
  private readonly movieGameService = inject(MovieGameService);
  private readonly destroyRef = inject(DestroyRef);

  view = signal<View>('list');
  items = signal<DataPointSummary[]>([]);
  tags = signal<TagResponse[]>([]);
  isLoading = signal(false);
  isSaving = signal(false);
  formErrors = signal<Record<string, string>>({});

  // Filters
  activeTypeFilter = signal<DataPointType | null>(null);
  activeTagIds = signal<Set<number>>(new Set());

  // Form state — shared tag selection
  selectedTagIds = signal<Set<number>>(new Set());

  // Photo form
  photoName = '';
  photoBase64 = signal<string>('');
  photoContentType = signal<string>('');
  photoPreview = signal<string>('');

  // Place form
  placeName = '';
  placeLat: number | null = null;
  placeLng: number | null = null;
  placeHint = '';
  private placeMap: L.Map | null = null;
  private placeMarker: L.Marker | null = null;

  // Movie form
  movieSearchQuery = '';
  movieSearchResults = signal<TmdbMovie[]>([]);
  selectedMovie = signal<TmdbMovie | null>(null);
  movieCharacterName = '';
  private readonly movieSearch$ = new Subject<string>();

  // Question form
  questionText = '';
  questionAnswer = '';

  // Edit form
  editingItem = signal<DataPointSummary | null>(null);
  editName = '';
  editCorrectAnswer = '';
  editHint = '';
  editQuestionText = '';
  editLat: number | null = null;
  editLng: number | null = null;
  private editMap: L.Map | null = null;
  private editMarker: L.Marker | null = null;

  typeFilters = [
    { type: 'PHOTO' as DataPointType, emoji: '📷', label: 'Photos' },
    { type: 'PLACE' as DataPointType, emoji: '📍', label: 'Places' },
    { type: 'MOVIE' as DataPointType, emoji: '🎬', label: 'Movies' },
    { type: 'QUESTION' as DataPointType, emoji: '❓', label: 'Questions' },
  ];

  questionSuggestions = [
    'Where were you born?',
    'What is your spouse\'s name?',
    'What is your youngest child\'s name?',
    'What school did you attend?',
    'What was your first job?',
    'What is your favorite food?',
    'What city did you grow up in?',
    'What is your pet\'s name?',
  ];

  filteredItems = signal<DataPointSummary[]>([]);

  ngOnInit() {
    this.loadTags();
    this.loadData();
    this.setupMovieSearch();
  }

  ngOnDestroy() {
    this.destroyPlaceMap();
    this.destroyEditMap();
  }

  private loadTags() {
    this.tagService.getTags(this.keycloakId).pipe(
      tap(tags => this.tags.set(tags)),
      catchError(() => of([])),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  loadData() {
    this.isLoading.set(true);
    const types = this.activeTypeFilter() ? [this.activeTypeFilter()!] : undefined;
    const tagIds = this.activeTagIds().size > 0 ? Array.from(this.activeTagIds()) : undefined;

    this.dataPointService.getAllDataPoints(this.keycloakId, types, tagIds).pipe(
      tap(data => {
        this.items.set(data);
        this.applyFilters();
      }),
      catchError(() => { this.items.set([]); this.filteredItems.set([]); return of([]); }),
      finalize(() => this.isLoading.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  private applyFilters() {
    this.filteredItems.set(this.items());
  }

  toggleTypeFilter(type: DataPointType | null) {
    this.activeTypeFilter.set(type);
    this.loadData();
  }

  toggleTagFilter(tagId: number) {
    const current = new Set(this.activeTagIds());
    if (current.has(tagId)) current.delete(tagId); else current.add(tagId);
    this.activeTagIds.set(current);
    this.loadData();
  }

  toggleSelectedTag(tagId: number) {
    const current = new Set(this.selectedTagIds());
    if (current.has(tagId)) current.delete(tagId); else current.add(tagId);
    this.selectedTagIds.set(current);
  }

  // ── Photo ──

  onPhotoSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) {
      this.formErrors.set({ imageBase64: 'Image must be under 5MB' });
      return;
    }
    this.formErrors.update(e => { const { imageBase64, ...rest } = e; return rest; });
    this.photoContentType.set(file.type);
    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result as string;
      this.photoPreview.set(result);
      this.photoBase64.set(result.split(',')[1]);
    };
    reader.readAsDataURL(file);
  }

  submitPhoto() {
    const result = photoSchema.safeParse({
      name: this.photoName.trim(),
      imageBase64: this.photoBase64(),
      contentType: this.photoContentType(),
    });
    if (!result.success) {
      this.formErrors.set(getFieldErrors(result));
      return;
    }
    this.formErrors.set({});
    this.isSaving.set(true);
    this.dataPointService.createPhoto(this.keycloakId, {
      name: this.photoName.trim(),
      imageBase64: this.photoBase64(),
      contentType: this.photoContentType(),
      tagIds: Array.from(this.selectedTagIds()),
    }).pipe(
      tap(() => { this.resetForm(); this.view.set('list'); this.loadData(); }),
      catchError(() => of(null)),
      finalize(() => this.isSaving.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  // ── Place ──

  initPlaceMap() {
    // Defer to next tick so the DOM element is rendered
    setTimeout(() => {
      if (this.placeMap || !this.placeMapContainer?.nativeElement) return;

      const iconDefault = L.icon({
        iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
        iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
        shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
        iconSize: [25, 41],
        iconAnchor: [12, 41],
        popupAnchor: [1, -34],
        shadowSize: [41, 41],
      });
      L.Marker.prototype.options.icon = iconDefault;

      this.placeMap = L.map(this.placeMapContainer.nativeElement).setView([36.8, 10.18], 12);

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
        maxZoom: 19,
      }).addTo(this.placeMap);

      this.placeMap.on('click', (e: L.LeafletMouseEvent) => {
        const { lat, lng } = e.latlng;
        this.placeLat = lat;
        this.placeLng = lng;

        if (this.placeMarker) {
          this.placeMarker.setLatLng(e.latlng);
        } else {
          this.placeMarker = L.marker(e.latlng).addTo(this.placeMap!);
        }
      });
    }, 0);
  }

  destroyPlaceMap() {
    if (this.placeMap) {
      this.placeMap.remove();
      this.placeMap = null;
      this.placeMarker = null;
    }
  }

  initEditPlaceMap() {
    setTimeout(() => {
      if (this.editMap || !this.editMapContainer?.nativeElement) return;

      const iconDefault = L.icon({
        iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
        iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
        shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
        iconSize: [25, 41],
        iconAnchor: [12, 41],
        popupAnchor: [1, -34],
        shadowSize: [41, 41],
      });
      L.Marker.prototype.options.icon = iconDefault;

      const lat = this.editLat ?? 36.8;
      const lng = this.editLng ?? 10.18;
      this.editMap = L.map(this.editMapContainer.nativeElement).setView([lat, lng], 14);

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
        maxZoom: 19,
      }).addTo(this.editMap);

      // Place existing marker
      if (this.editLat != null && this.editLng != null) {
        this.editMarker = L.marker([this.editLat, this.editLng]).addTo(this.editMap);
      }

      this.editMap.on('click', (e: L.LeafletMouseEvent) => {
        const { lat, lng } = e.latlng;
        this.editLat = lat;
        this.editLng = lng;

        if (this.editMarker) {
          this.editMarker.setLatLng(e.latlng);
        } else {
          this.editMarker = L.marker(e.latlng).addTo(this.editMap!);
        }
      });
    }, 0);
  }

  destroyEditMap() {
    if (this.editMap) {
      this.editMap.remove();
      this.editMap = null;
      this.editMarker = null;
    }
  }

  submitPlace() {
    const result = placeSchema.safeParse({
      name: this.placeName.trim(),
      latitude: this.placeLat,
      longitude: this.placeLng,
      hint: this.placeHint.trim() || '',
    });
    if (!result.success) {
      this.formErrors.set(getFieldErrors(result));
      return;
    }
    this.formErrors.set({});
    this.isSaving.set(true);
    this.destroyPlaceMap();
    this.dataPointService.createPlace(this.keycloakId, {
      name: this.placeName.trim(),
      latitude: this.placeLat!,
      longitude: this.placeLng!,
      hint: this.placeHint.trim() || undefined,
      tagIds: Array.from(this.selectedTagIds()),
    }).pipe(
      tap(() => { this.resetForm(); this.view.set('list'); this.loadData(); }),
      catchError(() => of(null)),
      finalize(() => this.isSaving.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  // ── Movie ──

  private setupMovieSearch() {
    this.movieSearch$.pipe(
      debounceTime(400),
      switchMap(query => query.trim().length > 1
        ? this.movieGameService.searchMovies(query).pipe(catchError(() => of([])))
        : of([])),
      tap(results => this.movieSearchResults.set(results)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  onMovieSearch(query: string) {
    this.movieSearch$.next(query);
  }

  selectMovie(movie: TmdbMovie) {
    this.selectedMovie.set(movie);
    this.movieSearchResults.set([]);
  }

  clearMovie() {
    this.selectedMovie.set(null);
    this.movieSearchQuery = '';
    this.movieCharacterName = '';
  }

  submitMovie() {
    const movie = this.selectedMovie();
    if (!movie) return;
    const result = movieMemorySchema.safeParse({
      originalTitle: movie.title,
      correctAnswer: this.movieCharacterName.trim(),
    });
    if (!result.success) {
      this.formErrors.set(getFieldErrors(result));
      return;
    }
    this.formErrors.set({});
    this.isSaving.set(true);
    this.dataPointService.createMovie(this.keycloakId, {
      tmdbId: movie.id,
      originalTitle: movie.title,
      posterPath: movie.poster_path || '',
      releaseDate: movie.release_date || '',
      correctAnswer: this.movieCharacterName.trim(),
      tagIds: Array.from(this.selectedTagIds()),
    }).pipe(
      tap(() => { this.resetForm(); this.view.set('list'); this.loadData(); }),
      catchError(() => of(null)),
      finalize(() => this.isSaving.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  // ── Question ──

  submitQuestion() {
    const result = questionMemorySchema.safeParse({
      questionText: this.questionText.trim(),
      correctAnswer: this.questionAnswer.trim(),
    });
    if (!result.success) {
      this.formErrors.set(getFieldErrors(result));
      return;
    }
    this.formErrors.set({});
    this.isSaving.set(true);
    this.dataPointService.createQuestion(this.keycloakId, {
      questionText: this.questionText.trim(),
      correctAnswer: this.questionAnswer.trim(),
      tagIds: Array.from(this.selectedTagIds()),
    }).pipe(
      tap(() => { this.resetForm(); this.view.set('list'); this.loadData(); }),
      catchError(() => of(null)),
      finalize(() => this.isSaving.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  // ── Edit ──

  openEdit(item: DataPointSummary) {
    this.editingItem.set(item);
    this.editName = item.label;
    this.editCorrectAnswer = item.correctAnswer || '';
    this.editHint = item.hint || '';
    this.editQuestionText = item.type === 'QUESTION' ? item.label : '';
    this.editLat = item.latitude ?? null;
    this.editLng = item.longitude ?? null;
    this.view.set('edit');
    if (item.type === 'PLACE') {
      this.initEditPlaceMap();
    }
  }

  isEditInvalid(): boolean {
    const ei = this.editingItem();
    if (!ei) return true;
    if (ei.type === 'PHOTO' && !this.editName.trim()) return true;
    if (ei.type === 'PLACE' && !this.editName.trim()) return true;
    if (ei.type === 'MOVIE' && !this.editCorrectAnswer.trim()) return true;
    if (ei.type === 'QUESTION' && (!this.editQuestionText.trim() || !this.editCorrectAnswer.trim())) return true;
    return false;
  }

  submitEdit() {
    const ei = this.editingItem();
    if (!ei || this.isEditInvalid()) return;
    this.isSaving.set(true);
    let obs: any;
    switch (ei.type) {
      case 'PHOTO':
        obs = this.dataPointService.updatePhoto(ei.id, { name: this.editName.trim() });
        break;
      case 'PLACE':
        obs = this.dataPointService.updatePlace(ei.id, {
          name: this.editName.trim(),
          hint: this.editHint.trim(),
          ...(this.editLat != null && this.editLng != null ? { latitude: this.editLat, longitude: this.editLng } : {}),
        });
        break;
      case 'MOVIE':
        obs = this.dataPointService.updateMovie(ei.id, { correctAnswer: this.editCorrectAnswer.trim() });
        break;
      case 'QUESTION':
        obs = this.dataPointService.updateQuestion(ei.id, { questionText: this.editQuestionText.trim(), correctAnswer: this.editCorrectAnswer.trim() });
        break;
      default: return;
    }
    obs.pipe(
      tap(() => { this.destroyEditMap(); this.editingItem.set(null); this.view.set('list'); this.loadData(); }),
      catchError(() => of(null)),
      finalize(() => this.isSaving.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  // ── Delete ──

  deleteItem(item: DataPointSummary) {
    let obs;
    switch (item.type) {
      case 'PHOTO': obs = this.dataPointService.deletePhoto(item.id); break;
      case 'PLACE': obs = this.dataPointService.deletePlace(item.id); break;
      case 'MOVIE': obs = this.dataPointService.deleteMovie(item.id); break;
      case 'QUESTION': obs = this.dataPointService.deleteQuestion(item.id); break;
      default: return;
    }
    obs.pipe(
      tap(() => this.items.update(items => { const f = items.filter(i => !(i.id === item.id && i.type === item.type)); this.filteredItems.set(f); return f; })),
      catchError(() => of(null)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  // ── Helpers ──

  private resetForm() {
    this.photoName = ''; this.photoBase64.set(''); this.photoPreview.set('');
    this.placeName = ''; this.placeLat = null; this.placeLng = null; this.placeHint = '';
    this.destroyPlaceMap();
    this.movieSearchQuery = ''; this.selectedMovie.set(null); this.movieCharacterName = '';
    this.questionText = ''; this.questionAnswer = '';
    this.selectedTagIds.set(new Set());
    this.formErrors.set({});
  }

  getTypeEmoji(type: DataPointType): string {
    const map: Record<DataPointType, string> = { PHOTO: '📷', PLACE: '📍', MOVIE: '🎬', QUESTION: '❓' };
    return map[type] || '📄';
  }

  getTypeBadgeClass(type: DataPointType): string {
    const map: Record<DataPointType, string> = {
      PHOTO: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300',
      PLACE: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300',
      MOVIE: 'bg-rose-100 text-rose-700 dark:bg-rose-900/30 dark:text-rose-300',
      QUESTION: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300',
    };
    return map[type] || '';
  }
}
