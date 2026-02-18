import {
  Component, Input, Output, EventEmitter, signal, inject, DestroyRef, OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, of, tap, finalize } from 'rxjs';

import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardButtonComponent } from '@/shared/components/button';
import { MemoryTagService, type TagResponse } from '@/core/services/memory-tag.service';

const PRESET_COLORS = [
  '#3b82f6', '#ef4444', '#10b981', '#f59e0b', '#8b5cf6',
  '#ec4899', '#06b6d4', '#6b7280', '#f97316', '#14b8a6',
];

@Component({
  selector: 'app-tag-manager',
  standalone: true,
  imports: [CommonModule, FormsModule, ZardCardComponent, ZardIconComponent, ZardButtonComponent],
  template: `
    <div class="space-y-6">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-2xl font-bold text-foreground">Tags</h2>
          <p class="text-muted-foreground">Create & manage tags like GitHub labels</p>
        </div>
        <z-button variant="outline" (click)="goBack.emit()">
          <z-icon zType="arrow-left" class="mr-2 h-4 w-4" /> Back
        </z-button>
      </div>

      <!-- Create Tag -->
      <z-card class="p-6">
        <h3 class="text-lg font-semibold mb-4">Create New Tag</h3>
        <div class="flex flex-wrap gap-3 items-end">
          <div class="flex-1 min-w-[200px]">
            <label class="text-sm font-medium text-foreground mb-1 block">Name</label>
            <input type="text" [(ngModel)]="newTagName"
              class="w-full rounded-md border border-border bg-background px-3 py-2 text-sm"
              placeholder="e.g., Work, School, Vacation..." />
          </div>
          <div>
            <label class="text-sm font-medium text-foreground mb-1 block">Color</label>
            <div class="flex gap-1.5">
              @for (color of presetColors; track color) {
                <button (click)="newTagColor = color"
                  class="w-7 h-7 rounded-full border-2 transition-all"
                  [style.background-color]="color"
                  [class.border-foreground]="newTagColor === color"
                  [class.border-transparent]="newTagColor !== color"
                  [class.scale-110]="newTagColor === color">
                </button>
              }
            </div>
          </div>
          <button z-button (click)="createTag()" [disabled]="!newTagName.trim() || isCreating()">
            <z-icon zType="plus" class="mr-2 h-4 w-4" /> Add Tag
          </button>
        </div>
      </z-card>

      <!-- Tag List -->
      <div class="space-y-2">
        @if (isLoading()) {
          <div class="text-center py-8 text-muted-foreground">Loading tags...</div>
        } @else if (tags().length === 0) {
          <div class="text-center py-8 text-muted-foreground">No tags yet. Create your first tag above!</div>
        } @else {
          <div class="flex flex-wrap gap-2">
            @for (tag of tags(); track tag.id) {
              <div class="group relative inline-flex items-center gap-2 px-4 py-2 rounded-full border border-border bg-background hover:bg-muted transition-colors">
                <span class="w-3 h-3 rounded-full" [style.background-color]="tag.color"></span>
                @if (editingTagId() === tag.id) {
                  <input type="text" [(ngModel)]="editTagName"
                    class="bg-transparent border-none text-sm font-medium focus:outline-none w-24"
                    (keydown.enter)="saveEdit(tag.id)"
                    (keydown.escape)="cancelEdit()" />
                  <button (click)="saveEdit(tag.id)" class="text-green-500 hover:text-green-700">
                    <z-icon zType="check" class="h-3.5 w-3.5" />
                  </button>
                  <button (click)="cancelEdit()" class="text-muted-foreground hover:text-foreground">
                    <z-icon zType="x" class="h-3.5 w-3.5" />
                  </button>
                } @else {
                  <span class="text-sm font-medium">{{ tag.name }}</span>
                  <button (click)="startEdit(tag)" class="opacity-0 group-hover:opacity-100 text-muted-foreground hover:text-foreground transition-opacity">
                    <z-icon zType="settings" class="h-3.5 w-3.5" />
                  </button>
                  <button (click)="deleteTag(tag.id)" class="opacity-0 group-hover:opacity-100 text-muted-foreground hover:text-red-500 transition-opacity">
                    <z-icon zType="trash-2" class="h-3.5 w-3.5" />
                  </button>
                }
              </div>
            }
          </div>
        }
      </div>
    </div>
  `,
})
export class TagManagerComponent implements OnInit {
  @Input({ required: true }) keycloakId!: string;
  @Output() goBack = new EventEmitter<void>();

  private readonly tagService = inject(MemoryTagService);
  private readonly destroyRef = inject(DestroyRef);

  tags = signal<TagResponse[]>([]);
  isLoading = signal(false);
  isCreating = signal(false);

  newTagName = '';
  newTagColor = '#3b82f6';
  presetColors = PRESET_COLORS;

  editingTagId = signal<number | null>(null);
  editTagName = '';
  editTagColor = '';

  ngOnInit() {
    this.loadTags();
  }

  loadTags() {
    this.isLoading.set(true);
    this.tagService.getTags(this.keycloakId).pipe(
      tap(tags => this.tags.set(tags)),
      catchError(() => of([])),
      finalize(() => this.isLoading.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  createTag() {
    if (!this.newTagName.trim()) return;
    this.isCreating.set(true);
    this.tagService.createTag(this.keycloakId, { name: this.newTagName.trim(), color: this.newTagColor }).pipe(
      tap(tag => {
        this.tags.update(t => [...t, tag]);
        this.newTagName = '';
      }),
      catchError(() => of(null)),
      finalize(() => this.isCreating.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  startEdit(tag: TagResponse) {
    this.editingTagId.set(tag.id);
    this.editTagName = tag.name;
    this.editTagColor = tag.color;
  }

  cancelEdit() {
    this.editingTagId.set(null);
  }

  saveEdit(tagId: number) {
    const tag = this.tags().find(t => t.id === tagId);
    if (!tag) return;
    this.tagService.updateTag(tagId, { name: this.editTagName.trim(), color: tag.color }).pipe(
      tap(updated => {
        this.tags.update(tags => tags.map(t => t.id === tagId ? updated : t));
        this.editingTagId.set(null);
      }),
      catchError(() => of(null)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  deleteTag(tagId: number) {
    this.tagService.deleteTag(tagId).pipe(
      tap(() => this.tags.update(tags => tags.filter(t => t.id !== tagId))),
      catchError(() => of(null)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }
}
