import {
  Component, Input, Output, EventEmitter, OnInit, signal, computed, effect
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent, type ZardIcon } from '@/shared/components/icon';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardTabGroupComponent, ZardTabComponent } from '@/shared/components/tabs';

import { DailyMonitoringService } from '@/core/services/daily-monitoring.service';
import {
  DailyLogResponse, AvailableMedication,
  NutritionEntryRequest, NutritionEntryResponse, MealType, QuantityLevel, AppetiteLevel,
  MedicationIntakeLogRequest, MedicationIntakeLogResponse, IntakeStatus,
  ActivityEntryRequest, ActivityEntryResponse, ActivityType, IntensityLevel,
  IncidentEntryRequest, IncidentEntryResponse, IncidentType, SeverityLevel,
} from '@/core/models/daily-monitoring.model';

// ─── Date helpers ─────────────────────────────────────────────────────────────

function todayIso(): string {
  return new Date().toISOString().split('T')[0];
}

function addDaysIso(iso: string, n: number): string {
  const d = new Date(iso + 'T12:00:00');
  d.setDate(d.getDate() + n);
  return d.toISOString().split('T')[0];
}

function getMondayOf(iso: string): string {
  const d = new Date(iso + 'T12:00:00');
  const day = d.getDay();
  const diff = day === 0 ? -6 : 1 - day;
  d.setDate(d.getDate() + diff);
  return d.toISOString().split('T')[0];
}

interface WeekDay {
  iso: string;
  shortLabel: string;
  dayNum: number;
  isFuture: boolean;
  isToday: boolean;
}

const DAY_LABELS_FR = ['Dim', 'Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam'];

function buildWeek(mondayIso: string, today: string): WeekDay[] {
  const days: WeekDay[] = [];
  for (let i = 0; i < 6; i++) {
    const iso = addDaysIso(mondayIso, i);
    const d = new Date(iso + 'T12:00:00');
    days.push({
      iso,
      shortLabel: DAY_LABELS_FR[d.getDay()],
      dayNum: d.getDate(),
      isFuture: iso > today,
      isToday: iso === today,
    });
  }
  return days;
}

// ─── Display config ───────────────────────────────────────────────────────────

const MEAL_LABELS: Record<MealType, string> = {
  BREAKFAST: 'Petit-déj', LUNCH: 'Déjeuner', DINNER: 'Dîner', SNACK: 'Collation',
};
const MEAL_ICONS: Record<MealType, ZardIcon> = {
  BREAKFAST: 'sun', LUNCH: 'circle', DINNER: 'moon', SNACK: 'star',
};

const INTAKE_STATUS_CONFIG: Record<IntakeStatus, { label: string; color: string; icon: ZardIcon }> = {
  PRIS:      { label: 'Pris',      color: 'bg-emerald-100 text-emerald-700 border border-emerald-200', icon: 'check' },
  OUBLIE:    { label: 'Oublié',    color: 'bg-amber-100 text-amber-700 border border-amber-200',       icon: 'triangle-alert' },
  REFUSE:    { label: 'Refusé',    color: 'bg-red-100 text-red-700 border border-red-200',             icon: 'x' },
  EN_RETARD: { label: 'En retard', color: 'bg-blue-100 text-blue-700 border border-blue-200',          icon: 'clock' },
};

const SEVERITY_CONFIG: Record<SeverityLevel, { label: string; color: string }> = {
  LEGER:  { label: 'Léger',  color: 'bg-amber-100 text-amber-700 border border-amber-200' },
  MODERE: { label: 'Modéré', color: 'bg-orange-100 text-orange-700 border border-orange-200' },
  GRAVE:  { label: 'Grave',  color: 'bg-red-100 text-red-700 border border-red-200' },
};

const ACTIVITY_TYPE_ICONS: Record<ActivityType, ZardIcon> = {
  PHYSIQUE: 'activity', COGNITIVE: 'brain', SOCIALE: 'users',
  HYGIENE: 'heart', PROMENADE: 'map-pin', AUTRE: 'star',
};

const INCIDENT_ICONS: Record<IncidentType, ZardIcon> = {
  CHUTE: 'triangle-alert', CONFUSION: 'info', AGITATION: 'zap',
  DEAMBULATION: 'map-pin', CRISE: 'bell', AUTRE: 'shield',
};

const PAGE_SIZE = 5;

// ─────────────────────────────────────────────────────────────────────────────

@Component({
  selector: 'app-suivi-quotidien',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    ZardCardComponent, ZardIconComponent, ZardBadgeComponent, ZardButtonComponent,
    ZardTabGroupComponent, ZardTabComponent,
  ],
  template: `
<!-- ══ HEADER ══ -->
<div class="flex items-center gap-3 mb-6">
  <button z-button zType="ghost" zSize="sm" (click)="goBack.emit()">
    <z-icon zType="arrow-left" />
  </button>
  <div>
    <h2 class="text-2xl font-bold tracking-tight">Suivi Quotidien</h2>
    <p class="text-sm text-muted-foreground">
      {{ formatDateFr(selectedDate()) }}
    </p>
  </div>
</div>

<!-- ══ WEEK NAVIGATOR ══ -->
<z-card class="p-4 mb-6">
  <div class="flex items-center gap-2">
    <button z-button zType="ghost" zSize="sm" (click)="prevWeek()">
      <z-icon zType="chevron-left" />
    </button>
    <div class="flex-1 grid grid-cols-6 gap-1">
      @for (day of weekDays(); track day.iso) {
        <button
          [disabled]="day.isFuture"
          (click)="selectDay(day.iso)"
          [class]="dayBtnClass(day)">
          <span class="text-[11px] font-semibold uppercase tracking-wide">{{ day.shortLabel }}</span>
          <span class="text-lg font-bold leading-tight">{{ day.dayNum }}</span>
          @if (day.isToday) {
            <span class="w-1.5 h-1.5 rounded-full bg-current"></span>
          }
          @if (logCache()[day.iso]; as cached) {
            <span class="text-[9px] opacity-75 leading-tight">
              {{ totalEntries(cached) }}
            </span>
          }
        </button>
      }
    </div>
    <button z-button zType="ghost" zSize="sm"
      [disabled]="isCurrentWeek()"
      (click)="nextWeek()">
      <z-icon zType="chevron-right" />
    </button>
  </div>
  <p class="text-center text-xs text-muted-foreground mt-2">
    Semaine du {{ weekDays()[0].dayNum }}/{{ weekMonthDisplay() }}
    @if (isCurrentWeek()) {
      <span class="ml-2 bg-primary/10 text-primary px-2 py-0.5 rounded-full text-[10px] font-medium">Semaine actuelle</span>
    }
  </p>
</z-card>

<!-- ══ LOADING ══ -->
@if (loading()) {
  <div class="flex flex-col items-center justify-center py-16 gap-3">
    <z-icon zType="loader-2" class="h-8 w-8 text-primary animate-spin" />
    <p class="text-sm text-muted-foreground">Chargement du journal...</p>
  </div>
} @else {

<!-- ══ SUMMARY CARDS ══ -->
<div class="grid grid-cols-2 md:grid-cols-4 gap-3 mb-6">
  <z-card class="p-4 border-l-4 border-l-orange-400">
    <p class="text-xs text-muted-foreground mb-1">Alimentation</p>
    <p class="text-2xl font-bold">{{ log()?.nutritionEntries?.length ?? 0 }}</p>
    <p class="text-xs text-muted-foreground">repas enregistrés</p>
  </z-card>
  <z-card class="p-4 border-l-4 border-l-blue-400">
    <p class="text-xs text-muted-foreground mb-1">Médicaments</p>
    <p class="text-2xl font-bold">{{ prisTaken() }}<span class="text-sm font-normal text-muted-foreground">/{{ log()?.medicationIntakes?.length ?? 0 }}</span></p>
    <p class="text-xs text-muted-foreground">prises confirmées</p>
  </z-card>
  <z-card class="p-4 border-l-4 border-l-green-400">
    <p class="text-xs text-muted-foreground mb-1">Activités</p>
    <p class="text-2xl font-bold">{{ totalActivityMin() }}<span class="text-sm font-normal text-muted-foreground"> min</span></p>
    <p class="text-xs text-muted-foreground">{{ log()?.activityEntries?.length ?? 0 }} activité(s)</p>
  </z-card>
  <z-card class="p-4 border-l-4 border-l-red-400">
    <p class="text-xs text-muted-foreground mb-1">Incidents</p>
    <p class="text-2xl font-bold" [class]="(log()?.incidentEntries?.length ?? 0) > 0 ? 'text-red-600' : ''">{{ log()?.incidentEntries?.length ?? 0 }}</p>
    <p class="text-xs text-muted-foreground">signalé(s)</p>
  </z-card>
</div>

<!-- ══ TABS ══ -->
<z-tab-group class="w-full">

  <!-- ─────────────── VUE JOURNÉE ─────────────── -->
  <z-tab label="📋 Vue Journée">
    <div class="pt-4 space-y-6">

      <!-- ── Alimentation ── -->
      <div>
        <div class="flex items-center justify-between mb-3">
          <h3 class="font-semibold text-base flex items-center gap-2">
            <span class="p-1.5 rounded-lg bg-orange-100 text-orange-600"><z-icon zType="star" class="h-4 w-4" /></span>
            Alimentation
            <span class="text-xs font-normal text-muted-foreground">({{ log()?.nutritionEntries?.length ?? 0 }} repas)</span>
          </h3>
          <button z-button zType="outline" zSize="sm" (click)="showNutritionForm.set(true)">
            <z-icon zType="plus" class="h-3.5 w-3.5 mr-1" /> Ajouter
          </button>
        </div>
        @if ((log()?.nutritionEntries?.length ?? 0) > 0) {
          <div class="grid gap-2">
            @for (e of log()!.nutritionEntries; track e.id) {
              <div class="flex items-center gap-3 p-3 rounded-xl border bg-card hover:bg-muted/40 transition-colors">
                <div class="p-2 rounded-lg bg-orange-100 text-orange-600 shrink-0">
                  <z-icon [zType]="mealIcon(e.mealType)" class="h-4 w-4" />
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="font-medium text-sm">{{ mealLabel(e.mealType) }}</span>
                    @if (e.entryTime) { <span class="text-xs text-muted-foreground">{{ e.entryTime }}</span> }
                    <span [class]="'text-xs px-1.5 py-0.5 rounded-full font-medium ' + quantityBadge(e.quantity)">{{ e.quantity }}</span>
                    <span [class]="'text-xs px-1.5 py-0.5 rounded-full font-medium ' + appetiteBadge(e.appetite)">App. {{ e.appetite }}</span>
                  </div>
                  <p class="text-xs text-muted-foreground truncate mt-0.5">{{ e.description }}</p>
                  @if (e.hydrationMl) { <p class="text-xs text-blue-500">💧 {{ e.hydrationMl }} ml</p> }
                </div>
                <div class="flex gap-1 shrink-0">
                  <button z-button zType="ghost" zSize="sm" (click)="editNutrition(e)"><z-icon zType="edit" class="h-3.5 w-3.5" /></button>
                  <button z-button zType="ghost" zSize="sm" class="text-destructive" (click)="deleteNutrition(e.id)"><z-icon zType="trash-2" class="h-3.5 w-3.5" /></button>
                </div>
              </div>
            }
          </div>
        } @else {
          <div class="text-center py-4 text-muted-foreground text-sm border border-dashed rounded-xl">Aucun repas enregistré</div>
        }
      </div>

      <div class="border-t border-dashed"></div>

      <!-- ── Médicaments ── -->
      <div>
        <div class="flex items-center justify-between mb-3">
          <h3 class="font-semibold text-base flex items-center gap-2">
            <span class="p-1.5 rounded-lg bg-blue-100 text-blue-600"><z-icon zType="pill" class="h-4 w-4" /></span>
            Médicaments
            <span class="text-xs font-normal text-muted-foreground">({{ prisTaken() }}/{{ log()?.medicationIntakes?.length ?? 0 }} pris)</span>
          </h3>
          <button z-button zType="outline" zSize="sm" (click)="showMedForm.set(true)">
            <z-icon zType="plus" class="h-3.5 w-3.5 mr-1" /> Ajouter
          </button>
        </div>
        @if ((log()?.medicationIntakes?.length ?? 0) > 0) {
          <div class="grid gap-2">
            @for (e of log()!.medicationIntakes; track e.id) {
              <div class="flex items-center gap-3 p-3 rounded-xl border bg-card hover:bg-muted/40 transition-colors">
                <div class="p-2 rounded-lg shrink-0" [class]="e.status === 'PRIS' ? 'bg-emerald-100 text-emerald-600' : e.status === 'OUBLIE' ? 'bg-amber-100 text-amber-600' : e.status === 'REFUSE' ? 'bg-red-100 text-red-600' : 'bg-blue-100 text-blue-600'">
                  <z-icon [zType]="intakeStatusConfig(e.status).icon" class="h-4 w-4" />
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="font-medium text-sm">{{ e.medicationName }}</span>
                    @if (e.dosage) { <span class="text-xs text-muted-foreground">{{ e.dosage }}</span> }
                    <span [class]="'text-xs px-1.5 py-0.5 rounded-full font-medium ' + intakeStatusConfig(e.status).color">{{ intakeStatusConfig(e.status).label }}</span>
                  </div>
                  @if (e.takenAt) { <p class="text-xs text-muted-foreground mt-0.5">Pris à {{ e.takenAt }}</p> }
                  @if (e.notes) { <p class="text-xs italic text-muted-foreground">{{ e.notes }}</p> }
                </div>
                <div class="flex gap-1 shrink-0">
                  <button z-button zType="ghost" zSize="sm" (click)="editMedication(e)"><z-icon zType="edit" class="h-3.5 w-3.5" /></button>
                  <button z-button zType="ghost" zSize="sm" class="text-destructive" (click)="deleteMedication(e.id)"><z-icon zType="trash-2" class="h-3.5 w-3.5" /></button>
                </div>
              </div>
            }
          </div>
        } @else {
          <div class="text-center py-4 text-muted-foreground text-sm border border-dashed rounded-xl">Aucune prise médicamenteuse</div>
        }
      </div>

      <div class="border-t border-dashed"></div>

      <!-- ── Activités ── -->
      <div>
        <div class="flex items-center justify-between mb-3">
          <h3 class="font-semibold text-base flex items-center gap-2">
            <span class="p-1.5 rounded-lg bg-green-100 text-green-600"><z-icon zType="activity" class="h-4 w-4" /></span>
            Activités
            <span class="text-xs font-normal text-muted-foreground">({{ totalActivityMin() }} min au total)</span>
          </h3>
          <button z-button zType="outline" zSize="sm" (click)="showActivityForm.set(true)">
            <z-icon zType="plus" class="h-3.5 w-3.5 mr-1" /> Ajouter
          </button>
        </div>
        @if ((log()?.activityEntries?.length ?? 0) > 0) {
          <div class="grid gap-2">
            @for (e of log()!.activityEntries; track e.id) {
              <div class="flex items-center gap-3 p-3 rounded-xl border bg-card hover:bg-muted/40 transition-colors">
                <div class="p-2 rounded-lg bg-green-100 text-green-600 shrink-0">
                  <z-icon [zType]="activityIcon(e.activityType)" class="h-4 w-4" />
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="font-medium text-sm">{{ e.description }}</span>
                    <span class="text-xs px-1.5 py-0.5 rounded-full bg-green-100 text-green-700 border border-green-200 font-medium">{{ e.activityType }}</span>
                    @if (e.intensity) { <span class="text-xs text-muted-foreground">{{ e.intensity }}</span> }
                  </div>
                  <div class="flex gap-3 mt-0.5 text-xs text-muted-foreground">
                    @if (e.durationMinutes) { <span><z-icon zType="clock" class="h-3 w-3 inline" /> {{ e.durationMinutes }} min</span> }
                    @if (e.startTime) { <span>Début : {{ e.startTime }}</span> }
                  </div>
                </div>
                <div class="flex gap-1 shrink-0">
                  <button z-button zType="ghost" zSize="sm" (click)="editActivity(e)"><z-icon zType="edit" class="h-3.5 w-3.5" /></button>
                  <button z-button zType="ghost" zSize="sm" class="text-destructive" (click)="deleteActivity(e.id)"><z-icon zType="trash-2" class="h-3.5 w-3.5" /></button>
                </div>
              </div>
            }
          </div>
        } @else {
          <div class="text-center py-4 text-muted-foreground text-sm border border-dashed rounded-xl">Aucune activité enregistrée</div>
        }
      </div>

      <div class="border-t border-dashed"></div>

      <!-- ── Incidents ── -->
      <div>
        <div class="flex items-center justify-between mb-3">
          <h3 class="font-semibold text-base flex items-center gap-2">
            <span class="p-1.5 rounded-lg bg-red-100 text-red-600"><z-icon zType="triangle-alert" class="h-4 w-4" /></span>
            Incidents
          </h3>
          <button z-button zType="outline" zSize="sm" class="border-red-200 text-red-600 hover:bg-red-50" (click)="showIncidentForm.set(true)">
            <z-icon zType="triangle-alert" class="h-3.5 w-3.5 mr-1" /> Signaler
          </button>
        </div>
        @if ((log()?.incidentEntries?.length ?? 0) > 0) {
          <div class="grid gap-2">
            @for (e of log()!.incidentEntries; track e.id) {
              <div class="flex items-center gap-3 p-3 rounded-xl border-l-4 border bg-card" [class]="incidentBorderColor(e.severity)">
                <div class="p-2 rounded-lg bg-red-100 text-red-600 shrink-0">
                  <z-icon [zType]="incidentIcon(e.incidentType)" class="h-4 w-4" />
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="font-medium text-sm">{{ e.incidentType }}</span>
                    <span [class]="'text-xs px-1.5 py-0.5 rounded-full font-medium ' + severityConfig(e.severity).color">{{ severityConfig(e.severity).label }}</span>
                    @if (e.occurredAt) { <span class="text-xs text-muted-foreground">{{ e.occurredAt }}</span> }
                  </div>
                  <p class="text-xs text-muted-foreground truncate mt-0.5">{{ e.description }}</p>
                  @if (e.location) { <p class="text-xs text-muted-foreground"><z-icon zType="map-pin" class="h-3 w-3 inline" /> {{ e.location }}</p> }
                  @if (e.actionTaken) { <p class="text-xs text-green-700"><z-icon zType="check" class="h-3 w-3 inline" /> {{ e.actionTaken }}</p> }
                </div>
                <div class="flex gap-1 shrink-0">
                  <button z-button zType="ghost" zSize="sm" (click)="editIncident(e)"><z-icon zType="edit" class="h-3.5 w-3.5" /></button>
                  <button z-button zType="ghost" zSize="sm" class="text-destructive" (click)="deleteIncident(e.id)"><z-icon zType="trash-2" class="h-3.5 w-3.5" /></button>
                </div>
              </div>
            }
          </div>
        } @else {
          <div class="flex items-center justify-center gap-2 py-4 text-green-600 text-sm border border-dashed border-green-200 rounded-xl bg-green-50/50">
            <z-icon zType="shield" class="h-4 w-4" /> Aucun incident — journée calme !
          </div>
        }
      </div>

      <!-- ── Quick-add forms (modal-like inline) ── -->
      @if (showNutritionForm() || showMedForm() || showActivityForm() || showIncidentForm()) {
        <div class="fixed inset-0 bg-black/40 z-40 flex items-end md:items-center justify-center p-4" (click)="closeAllForms()">
          <div class="w-full max-w-lg max-h-[85vh] overflow-y-auto" (click)="$event.stopPropagation()">
            @if (showNutritionForm()) { <ng-container *ngTemplateOutlet="nutritionFormTpl" /> }
            @if (showMedForm())       { <ng-container *ngTemplateOutlet="medFormTpl" /> }
            @if (showActivityForm())  { <ng-container *ngTemplateOutlet="activityFormTpl" /> }
            @if (showIncidentForm())  { <ng-container *ngTemplateOutlet="incidentFormTpl" /> }
          </div>
        </div>
      }
    </div>
  </z-tab>

  <!-- ─────────────── ALIMENTATION ─────────────── -->
  <z-tab label="🍽️ Alimentation">
    <div class="pt-4 space-y-3">
      <!-- Search + sort bar -->
      <div class="flex gap-2 flex-wrap">
        <input
          [value]="nSearch()"
          (input)="nSearch.set(getInputValue($event))"
          placeholder="Rechercher un repas..."
          class="flex-1 min-w-[150px] px-3 py-2 text-sm border border-border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-primary" />
        <select
          [value]="nSort()"
          (change)="nSort.set(getInputValue($event))"
          class="px-3 py-2 text-sm border border-border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-primary">
          <option value="">Trier par...</option>
          <option value="time">Heure</option>
          <option value="type">Type repas</option>
          <option value="quantity">Quantité</option>
        </select>
        <button z-button zType="outline" zSize="sm" (click)="showNutritionForm.set(true)">
          <z-icon zType="plus" class="h-3.5 w-3.5 mr-1" /> Ajouter
        </button>
      </div>
      @if (nSearch()) {
        <p class="text-xs text-muted-foreground">{{ nFilteredSorted().length }} résultat(s) pour "{{ nSearch() }}"
          <button class="ml-1 text-primary underline" (click)="nSearch.set('')">Effacer</button>
        </p>
      }
      @if (nFilteredSorted().length > 0) {
        @for (entry of nPageItems(); track entry.id) {
          <z-card class="p-4">
            <div class="flex items-start justify-between gap-3">
              <div class="flex gap-3">
                <div class="p-2 rounded-lg bg-orange-100 text-orange-600 shrink-0">
                  <z-icon [zType]="mealIcon(entry.mealType)" class="h-4 w-4" />
                </div>
                <div>
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="font-semibold text-sm">{{ mealLabel(entry.mealType) }}</span>
                    @if (entry.entryTime) { <span class="text-xs text-muted-foreground">{{ entry.entryTime }}</span> }
                    <span [class]="'text-xs px-2 py-0.5 rounded-full font-medium ' + quantityBadge(entry.quantity)">{{ entry.quantity }}</span>
                    <span [class]="'text-xs px-2 py-0.5 rounded-full font-medium ' + appetiteBadge(entry.appetite)">App. {{ entry.appetite }}</span>
                  </div>
                  <p class="text-sm text-muted-foreground mt-1">{{ entry.description }}</p>
                  @if (entry.hydrationMl) { <p class="text-xs text-blue-600 mt-1">💧 {{ entry.hydrationMl }} ml</p> }
                  @if (entry.notes) { <p class="text-xs italic text-muted-foreground mt-1">{{ entry.notes }}</p> }
                </div>
              </div>
              <div class="flex gap-1 shrink-0">
                <button z-button zType="ghost" zSize="sm" (click)="editNutrition(entry)"><z-icon zType="edit" class="h-3.5 w-3.5" /></button>
                <button z-button zType="ghost" zSize="sm" class="text-destructive" (click)="deleteNutrition(entry.id)"><z-icon zType="trash-2" class="h-3.5 w-3.5" /></button>
              </div>
            </div>
          </z-card>
        }
        <ng-container *ngTemplateOutlet="paginationTpl; context:{total:nFilteredSorted().length, page:nPage(), fn:'n'}" />
      } @else {
        <div class="text-center py-10 text-muted-foreground">
          <z-icon zType="star" class="mx-auto h-10 w-10 mb-3 opacity-30" />
          <p class="text-sm">{{ nSearch() ? 'Aucun résultat trouvé.' : 'Aucun repas enregistré.' }}</p>
        </div>
      }
      <!-- inline form visible when tab open -->
      @if (showNutritionForm()) {
        <ng-container *ngTemplateOutlet="nutritionFormTpl" />
      }
    </div>
  </z-tab>

  <!-- ─────────────── MÉDICAMENTS ─────────────── -->
  <z-tab label="💊 Médicaments">
    <div class="pt-4 space-y-3">
      <div class="flex gap-2 flex-wrap">
        <input
          [value]="mSearch()"
          (input)="mSearch.set(getInputValue($event))"
          placeholder="Rechercher un médicament..."
          class="flex-1 min-w-[150px] px-3 py-2 text-sm border border-border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-primary" />
        <select
          [value]="mSort()"
          (change)="mSort.set(getInputValue($event))"
          class="px-3 py-2 text-sm border border-border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-primary">
          <option value="">Trier par...</option>
          <option value="name">Nom</option>
          <option value="status">Statut</option>
          <option value="time">Heure</option>
        </select>
        <button z-button zType="outline" zSize="sm" (click)="showMedForm.set(true)">
          <z-icon zType="plus" class="h-3.5 w-3.5 mr-1" /> Ajouter
        </button>
      </div>
      @if (mSearch()) {
        <p class="text-xs text-muted-foreground">{{ mFilteredSorted().length }} résultat(s) pour "{{ mSearch() }}"
          <button class="ml-1 text-primary underline" (click)="mSearch.set('')">Effacer</button>
        </p>
      }
      @if (mFilteredSorted().length > 0) {
        @for (entry of mPageItems(); track entry.id) {
          <z-card class="p-4">
            <div class="flex items-start justify-between gap-3">
              <div class="flex gap-3">
                <div class="p-2 rounded-lg bg-blue-100 text-blue-600 shrink-0">
                  <z-icon [zType]="intakeStatusConfig(entry.status).icon" class="h-4 w-4" />
                </div>
                <div>
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="font-semibold text-sm">{{ entry.medicationName }}</span>
                    @if (entry.dosage) { <span class="text-xs text-muted-foreground">{{ entry.dosage }}</span> }
                    @if (entry.frequency) { <span class="text-xs text-muted-foreground">· {{ entry.frequency }}</span> }
                    <span [class]="'text-xs px-2 py-0.5 rounded-full font-medium ' + intakeStatusConfig(entry.status).color">{{ intakeStatusConfig(entry.status).label }}</span>
                  </div>
                  @if (entry.takenAt) { <p class="text-xs text-muted-foreground mt-1">Pris à {{ entry.takenAt }}</p> }
                  @if (entry.notes) { <p class="text-xs italic text-muted-foreground mt-1">{{ entry.notes }}</p> }
                </div>
              </div>
              <div class="flex gap-1 shrink-0">
                <button z-button zType="ghost" zSize="sm" (click)="editMedication(entry)"><z-icon zType="edit" class="h-3.5 w-3.5" /></button>
                <button z-button zType="ghost" zSize="sm" class="text-destructive" (click)="deleteMedication(entry.id)"><z-icon zType="trash-2" class="h-3.5 w-3.5" /></button>
              </div>
            </div>
          </z-card>
        }
        <ng-container *ngTemplateOutlet="paginationTpl; context:{total:mFilteredSorted().length, page:mPage(), fn:'m'}" />
      } @else {
        <div class="text-center py-10 text-muted-foreground">
          <z-icon zType="pill" class="mx-auto h-10 w-10 mb-3 opacity-30" />
          <p class="text-sm">{{ mSearch() ? 'Aucun résultat trouvé.' : 'Aucune prise médicamenteuse.' }}</p>
        </div>
      }
      @if (showMedForm()) {
        <ng-container *ngTemplateOutlet="medFormTpl" />
      }
    </div>
  </z-tab>

  <!-- ─────────────── ACTIVITÉS ─────────────── -->
  <z-tab label="🏃 Activités">
    <div class="pt-4 space-y-3">
      <div class="flex gap-2 flex-wrap">
        <input
          [value]="aSearch()"
          (input)="aSearch.set(getInputValue($event))"
          placeholder="Rechercher une activité..."
          class="flex-1 min-w-[150px] px-3 py-2 text-sm border border-border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-primary" />
        <select
          [value]="aSort()"
          (change)="aSort.set(getInputValue($event))"
          class="px-3 py-2 text-sm border border-border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-primary">
          <option value="">Trier par...</option>
          <option value="type">Type</option>
          <option value="duration">Durée ↓</option>
          <option value="intensity">Intensité</option>
          <option value="time">Heure</option>
        </select>
        <button z-button zType="outline" zSize="sm" (click)="showActivityForm.set(true)">
          <z-icon zType="plus" class="h-3.5 w-3.5 mr-1" /> Ajouter
        </button>
      </div>
      @if (aSearch()) {
        <p class="text-xs text-muted-foreground">{{ aFilteredSorted().length }} résultat(s) pour "{{ aSearch() }}"
          <button class="ml-1 text-primary underline" (click)="aSearch.set('')">Effacer</button>
        </p>
      }
      @if (aFilteredSorted().length > 0) {
        @for (entry of aPageItems(); track entry.id) {
          <z-card class="p-4">
            <div class="flex items-start justify-between gap-3">
              <div class="flex gap-3">
                <div class="p-2 rounded-lg bg-green-100 text-green-600 shrink-0">
                  <z-icon [zType]="activityIcon(entry.activityType)" class="h-4 w-4" />
                </div>
                <div>
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="font-semibold text-sm">{{ entry.description }}</span>
                    <span class="text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-700 border border-green-200 font-medium">{{ entry.activityType }}</span>
                    @if (entry.intensity) { <span class="text-xs text-muted-foreground">{{ entry.intensity }}</span> }
                  </div>
                  <div class="flex gap-3 mt-1 text-xs text-muted-foreground">
                    @if (entry.durationMinutes) { <span><z-icon zType="clock" class="h-3 w-3 inline" /> {{ entry.durationMinutes }} min</span> }
                    @if (entry.startTime) { <span>Début : {{ entry.startTime }}</span> }
                  </div>
                  @if (entry.notes) { <p class="text-xs italic text-muted-foreground mt-1">{{ entry.notes }}</p> }
                </div>
              </div>
              <div class="flex gap-1 shrink-0">
                <button z-button zType="ghost" zSize="sm" (click)="editActivity(entry)"><z-icon zType="edit" class="h-3.5 w-3.5" /></button>
                <button z-button zType="ghost" zSize="sm" class="text-destructive" (click)="deleteActivity(entry.id)"><z-icon zType="trash-2" class="h-3.5 w-3.5" /></button>
              </div>
            </div>
          </z-card>
        }
        <ng-container *ngTemplateOutlet="paginationTpl; context:{total:aFilteredSorted().length, page:aPage(), fn:'a'}" />
      } @else {
        <div class="text-center py-10 text-muted-foreground">
          <z-icon zType="activity" class="mx-auto h-10 w-10 mb-3 opacity-30" />
          <p class="text-sm">{{ aSearch() ? 'Aucun résultat trouvé.' : 'Aucune activité enregistrée.' }}</p>
        </div>
      }
      @if (showActivityForm()) {
        <ng-container *ngTemplateOutlet="activityFormTpl" />
      }
    </div>
  </z-tab>

  <!-- ─────────────── INCIDENTS ─────────────── -->
  <z-tab label="⚠️ Incidents">
    <div class="pt-4 space-y-3">
      <div class="flex gap-2 flex-wrap">
        <input
          [value]="iSearch()"
          (input)="iSearch.set(getInputValue($event))"
          placeholder="Rechercher un incident..."
          class="flex-1 min-w-[150px] px-3 py-2 text-sm border border-border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-primary" />
        <select
          [value]="iSort()"
          (change)="iSort.set(getInputValue($event))"
          class="px-3 py-2 text-sm border border-border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-primary">
          <option value="">Trier par...</option>
          <option value="severity">Gravité ↓</option>
          <option value="type">Type</option>
          <option value="time">Heure</option>
        </select>
        <button z-button zType="outline" zSize="sm" class="border-red-200 text-red-600 hover:bg-red-50"
          (click)="showIncidentForm.set(true)">
          <z-icon zType="triangle-alert" class="h-3.5 w-3.5 mr-1" /> Signaler
        </button>
      </div>
      @if (iSearch()) {
        <p class="text-xs text-muted-foreground">{{ iFilteredSorted().length }} résultat(s) pour "{{ iSearch() }}"
          <button class="ml-1 text-primary underline" (click)="iSearch.set('')">Effacer</button>
        </p>
      }
      @if (iFilteredSorted().length > 0) {
        @for (entry of iPageItems(); track entry.id) {
          <z-card class="p-4 border-l-4" [class]="incidentBorderColor(entry.severity)">
            <div class="flex items-start justify-between gap-3">
              <div class="flex gap-3">
                <div class="p-2 rounded-lg bg-red-100 text-red-600 shrink-0">
                  <z-icon [zType]="incidentIcon(entry.incidentType)" class="h-4 w-4" />
                </div>
                <div>
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="font-semibold text-sm">{{ entry.incidentType }}</span>
                    <span [class]="'text-xs px-2 py-0.5 rounded-full font-medium ' + severityConfig(entry.severity).color">{{ severityConfig(entry.severity).label }}</span>
                    @if (entry.occurredAt) { <span class="text-xs text-muted-foreground">{{ entry.occurredAt }}</span> }
                  </div>
                  <p class="text-sm text-muted-foreground mt-1">{{ entry.description }}</p>
                  @if (entry.location) { <p class="text-xs text-muted-foreground mt-1"><z-icon zType="map-pin" class="h-3 w-3 inline" /> {{ entry.location }}</p> }
                  @if (entry.injuryDetails) { <p class="text-xs text-red-600 mt-1">Blessure : {{ entry.injuryDetails }}</p> }
                  @if (entry.actionTaken) { <p class="text-xs text-green-700 mt-1"><z-icon zType="check" class="h-3 w-3 inline" /> {{ entry.actionTaken }}</p> }
                </div>
              </div>
              <div class="flex gap-1 shrink-0">
                <button z-button zType="ghost" zSize="sm" (click)="editIncident(entry)"><z-icon zType="edit" class="h-3.5 w-3.5" /></button>
                <button z-button zType="ghost" zSize="sm" class="text-destructive" (click)="deleteIncident(entry.id)"><z-icon zType="trash-2" class="h-3.5 w-3.5" /></button>
              </div>
            </div>
          </z-card>
        }
        <ng-container *ngTemplateOutlet="paginationTpl; context:{total:iFilteredSorted().length, page:iPage(), fn:'i'}" />
      } @else {
        <div class="flex items-center justify-center gap-2 py-10 text-green-600 text-sm">
          <z-icon zType="shield" class="h-5 w-5" />
          {{ iSearch() ? 'Aucun résultat trouvé.' : 'Aucun incident — journée calme !' }}
        </div>
      }
      @if (showIncidentForm()) {
        <ng-container *ngTemplateOutlet="incidentFormTpl" />
      }
    </div>
  </z-tab>

</z-tab-group>

} <!-- /loading -->

<!-- ══ PAGINATION TEMPLATE ══ -->
<ng-template #paginationTpl let-total="total" let-page="page" let-fn="fn">
  @if (total > pageSize) {
    <div class="flex items-center justify-between pt-3 border-t border-border/50">
      <span class="text-xs text-muted-foreground">
        {{ (page - 1) * pageSize + 1 }}–{{ min(page * pageSize, total) }} sur {{ total }} résultats
      </span>
      <div class="flex items-center gap-1">
        <button z-button zType="outline" zSize="sm" [disabled]="page <= 1" (click)="changePage(fn, page - 1)">
          <z-icon zType="chevron-left" class="h-3.5 w-3.5" />
        </button>
        <span class="px-2 text-xs font-medium">{{ page }} / {{ totalPages(total) }}</span>
        <button z-button zType="outline" zSize="sm" [disabled]="page >= totalPages(total)" (click)="changePage(fn, page + 1)">
          <z-icon zType="chevron-right" class="h-3.5 w-3.5" />
        </button>
      </div>
    </div>
  }
</ng-template>

<!-- ══ FORM TEMPLATES ══ -->

<!-- Nutrition form -->
<ng-template #nutritionFormTpl>
  <z-card class="p-5 border-primary/30 bg-primary/5 rounded-2xl shadow-lg">
    <div class="flex items-center justify-between mb-4">
      <h4 class="font-semibold flex items-center gap-2 text-primary">
        <z-icon zType="star" class="h-4 w-4" />
        {{ editNutritionId() ? 'Modifier le repas' : 'Nouveau repas' }}
      </h4>
      <button z-button zType="ghost" zSize="sm" (click)="cancelNutritionForm()"><z-icon zType="x" class="h-4 w-4" /></button>
    </div>
    <div class="grid gap-3 md:grid-cols-2">
      <div>
        <label class="text-xs font-medium text-muted-foreground mb-1 block">Type *</label>
        <select [(ngModel)]="nForm.mealType" class="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm focus:ring-2 focus:ring-primary focus:outline-none">
          <option value="BREAKFAST">🌅 Petit-déjeuner</option>
          <option value="LUNCH">☀️ Déjeuner</option>
          <option value="DINNER">🌙 Dîner</option>
          <option value="SNACK">⭐ Collation</option>
        </select>
      </div>
      <div>
        <label class="text-xs font-medium text-muted-foreground mb-1 block">Heure</label>
        <input type="time" [(ngModel)]="nForm.entryTime" class="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm focus:ring-2 focus:ring-primary focus:outline-none" />
      </div>
      <div class="md:col-span-2">
        <label class="text-xs font-medium text-muted-foreground mb-1 block">Aliments *</label>
        <input type="text" [(ngModel)]="nForm.description" placeholder="Ex: Soupe de légumes, pain complet..."
          class="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm focus:ring-2 focus:ring-primary focus:outline-none" />
      </div>
      <div>
        <label class="text-xs font-medium text-muted-foreground mb-1 block">Quantité consommée</label>
        <div class="flex gap-1 flex-wrap">
          @for (opt of [['COMPLET','✅ Tout'],['DEMI','½ Moitié'],['PEU','🔸 Peu'],['RIEN','❌ Rien']]; track opt[0]) {
            <button type="button" (click)="nForm.quantity = $any(opt[0])"
              [class]="'px-2 py-1.5 text-xs rounded-lg border font-medium transition-all ' + (nForm.quantity===opt[0] ? 'bg-primary text-primary-foreground border-primary shadow-sm' : 'border-border hover:border-primary/50')">
              {{ opt[1] }}
            </button>
          }
        </div>
      </div>
      <div>
        <label class="text-xs font-medium text-muted-foreground mb-1 block">Appétit</label>
        <div class="flex gap-1.5">
          @for (opt of [['BON','😊 Bon'],['MOYEN','😐 Moyen'],['FAIBLE','😟 Faible']]; track opt[0]) {
            <button type="button" (click)="nForm.appetite = $any(opt[0])"
              [class]="'flex-1 py-2 text-xs rounded-lg border font-medium transition-all ' + (nForm.appetite===opt[0] ? 'bg-primary text-primary-foreground border-primary shadow-sm' : 'border-border hover:border-primary/50')">
              {{ opt[1] }}
            </button>
          }
        </div>
      </div>
      <div>
        <label class="text-xs font-medium text-muted-foreground mb-1 block">💧 Hydratation (ml)</label>
        <input type="number" [(ngModel)]="nForm.hydrationMl" placeholder="200" min="0" max="2000"
          class="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm focus:ring-2 focus:ring-primary focus:outline-none" />
      </div>
      <div>
        <label class="text-xs font-medium text-muted-foreground mb-1 block">Notes</label>
        <input type="text" [(ngModel)]="nForm.notes" placeholder="Observations..."
          class="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm focus:ring-2 focus:ring-primary focus:outline-none" />
      </div>
    </div>
    <div class="flex gap-2 mt-4">
      <button z-button [disabled]="saving() || !nForm.description" (click)="saveNutrition()">
        @if (saving()) { <z-icon zType="loader-2" class="mr-2 animate-spin" /> } Enregistrer
      </button>
      <button z-button zType="outline" (click)="cancelNutritionForm()">Annuler</button>
    </div>
  </z-card>
</ng-template>

<!-- Medication form -->
<ng-template #medFormTpl>
  <z-card class="p-5 border-blue-200 bg-blue-50/50 rounded-2xl shadow-lg">
    <div class="flex items-center justify-between mb-4">
      <h4 class="font-semibold flex items-center gap-2 text-blue-700">
        <z-icon zType="pill" class="h-4 w-4" />
        {{ editMedId() ? 'Modifier la prise' : 'Enregistrer une prise' }}
      </h4>
      <button z-button zType="ghost" zSize="sm" (click)="cancelMedForm()"><z-icon zType="x" class="h-4 w-4" /></button>
    </div>
    @if (availableMeds().length === 0) {
      <div class="p-3 rounded-lg bg-amber-50 border border-amber-200 text-amber-700 text-sm mb-3 flex items-center gap-2">
        <z-icon zType="triangle-alert" class="h-4 w-4 shrink-0" />
        Aucun médicament prescrit trouvé pour ce patient.
      </div>
    }
    <div class="grid gap-3 md:grid-cols-2">
      <div class="md:col-span-2">
        <label class="text-xs font-medium text-muted-foreground mb-1 block">Médicament prescrit *</label>
        <select [(ngModel)]="mForm.medicationId" class="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm focus:ring-2 focus:ring-primary focus:outline-none">
          <option [ngValue]="undefined">-- Choisir un médicament --</option>
          @for (med of availableMeds(); track med.id) {
            <option [ngValue]="med.id">{{ med.medicationName }}{{ med.dosage ? ' – ' + med.dosage : '' }}{{ med.frequency ? ' (' + med.frequency + ')' : '' }}</option>
          }
        </select>
      </div>
      <div>
        <label class="text-xs font-medium text-muted-foreground mb-1 block">Heure de prise</label>
        <input type="time" [(ngModel)]="mForm.takenAt" class="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm focus:ring-2 focus:ring-primary focus:outline-none" />
      </div>
      <div>
        <label class="text-xs font-medium text-muted-foreground mb-1 block">Statut *</label>
        <div class="grid grid-cols-2 gap-1.5">
          @for (s of intakeStatuses; track s.value) {
            <button type="button" (click)="mForm.status = s.value"
              [class]="'py-2 px-2 text-xs rounded-lg border font-medium flex items-center gap-1.5 transition-all ' + (mForm.status===s.value ? 'bg-primary text-primary-foreground border-primary shadow-sm' : 'border-border hover:border-primary/50')">
              <z-icon [zType]="s.icon" class="h-3.5 w-3.5" />{{ s.label }}
            </button>
          }
        </div>
      </div>
      <div class="md:col-span-2">
        <label class="text-xs font-medium text-muted-foreground mb-1 block">Notes</label>
        <input type="text" [(ngModel)]="mForm.notes" placeholder="Observations..."
          class="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm focus:ring-2 focus:ring-primary focus:outline-none" />
      </div>
    </div>
    <div class="flex gap-2 mt-4">
      <button z-button [disabled]="saving() || !mForm.medicationId" (click)="saveMedication()">
        @if (saving()) { <z-icon zType="loader-2" class="mr-2 animate-spin" /> } Enregistrer
      </button>
      <button z-button zType="outline" (click)="cancelMedForm()">Annuler</button>
    </div>
  </z-card>
</ng-template>

<!-- Activity form -->
<ng-template #activityFormTpl>
  <z-card class="p-5 border-green-200 bg-green-50/50 rounded-2xl shadow-lg">
    <div class="flex items-center justify-between mb-4">
      <h4 class="font-semibold flex items-center gap-2 text-green-700">
        <z-icon zType="activity" class="h-4 w-4" />
        {{ editActivityId() ? 'Modifier l\'activité' : 'Nouvelle activité' }}
      </h4>
      <button z-button zType="ghost" zSize="sm" (click)="cancelActivityForm()"><z-icon zType="x" class="h-4 w-4" /></button>
    </div>
    <div class="grid gap-3 md:grid-cols-2">
      <div class="md:col-span-2">
        <label class="text-xs font-medium text-muted-foreground mb-1 block">Type *</label>
        <div class="grid grid-cols-3 md:grid-cols-6 gap-1.5">
          @for (t of activityTypes; track t.value) {
            <button type="button" (click)="aForm.activityType = t.value"
              [class]="'py-2 px-1 text-xs rounded-lg border font-medium flex flex-col items-center gap-1 transition-all ' + (aForm.activityType===t.value ? 'bg-primary text-primary-foreground border-primary shadow-sm' : 'border-border hover:border-primary/40')">
              <z-icon [zType]="t.icon" class="h-4 w-4" />{{ t.label }}
            </button>
          }
        </div>
      </div>
      <div class="md:col-span-2">
        <label class="text-xs font-medium text-muted-foreground mb-1 block">Description *</label>
        <input type="text" [(ngModel)]="aForm.description" placeholder="Ex: Marche dans le jardin..."
          class="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm focus:ring-2 focus:ring-primary focus:outline-none" />
      </div>
      <div>
        <label class="text-xs font-medium text-muted-foreground mb-1 block">Durée (minutes)</label>
        <input type="number" [(ngModel)]="aForm.durationMinutes" placeholder="30" min="1"
          class="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm focus:ring-2 focus:ring-primary focus:outline-none" />
      </div>
      <div>
        <label class="text-xs font-medium text-muted-foreground mb-1 block">Heure de début</label>
        <input type="time" [(ngModel)]="aForm.startTime"
          class="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm focus:ring-2 focus:ring-primary focus:outline-none" />
      </div>
      <div class="md:col-span-2">
        <label class="text-xs font-medium text-muted-foreground mb-1 block">Intensité</label>
        <div class="flex gap-2">
          @for (opt of [['FAIBLE','🟢 Faible'],['MODERE','🟡 Modérée'],['ELEVE','🔴 Élevée']]; track opt[0]) {
            <button type="button" (click)="aForm.intensity = $any(opt[0])"
              [class]="'flex-1 py-2 text-xs rounded-lg border font-medium transition-all ' + (aForm.intensity===opt[0] ? 'bg-primary text-primary-foreground border-primary shadow-sm' : 'border-border hover:border-primary/40')">{{ opt[1] }}</button>
          }
        </div>
      </div>
      <div class="md:col-span-2">
        <label class="text-xs font-medium text-muted-foreground mb-1 block">Notes</label>
        <input type="text" [(ngModel)]="aForm.notes" placeholder="Observations..."
          class="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm focus:ring-2 focus:ring-primary focus:outline-none" />
      </div>
    </div>
    <div class="flex gap-2 mt-4">
      <button z-button [disabled]="saving() || !aForm.description" (click)="saveActivity()">
        @if (saving()) { <z-icon zType="loader-2" class="mr-2 animate-spin" /> } Enregistrer
      </button>
      <button z-button zType="outline" (click)="cancelActivityForm()">Annuler</button>
    </div>
  </z-card>
</ng-template>

<!-- Incident form -->
<ng-template #incidentFormTpl>
  <z-card class="p-5 border-red-200 bg-red-50/50 rounded-2xl shadow-lg">
    <div class="flex items-center justify-between mb-4">
      <h4 class="font-semibold flex items-center gap-2 text-red-700">
        <z-icon zType="triangle-alert" class="h-4 w-4" />
        {{ editIncidentId() ? 'Modifier l\'incident' : 'Signaler un incident' }}
      </h4>
      <button z-button zType="ghost" zSize="sm" (click)="cancelIncidentForm()"><z-icon zType="x" class="h-4 w-4" /></button>
    </div>
    <div class="grid gap-3 md:grid-cols-2">
      <div class="md:col-span-2">
        <label class="text-xs font-medium text-muted-foreground mb-1 block">Type *</label>
        <div class="grid grid-cols-3 md:grid-cols-6 gap-1.5">
          @for (t of incidentTypes; track t.value) {
            <button type="button" (click)="iForm.incidentType = t.value"
              [class]="'py-2 px-1 text-xs rounded-lg border font-medium flex flex-col items-center gap-1 transition-all ' + (iForm.incidentType===t.value ? 'bg-red-600 text-white border-red-600 shadow-sm' : 'border-border hover:border-red-300')">
              <z-icon [zType]="t.icon" class="h-4 w-4" />{{ t.label }}
            </button>
          }
        </div>
      </div>
      <div class="md:col-span-2">
        <label class="text-xs font-medium text-muted-foreground mb-1 block">Description *</label>
        <input type="text" [(ngModel)]="iForm.description" placeholder="Décrivez l'incident..."
          class="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm focus:ring-2 focus:ring-red-400 focus:outline-none" />
      </div>
      <div>
        <label class="text-xs font-medium text-muted-foreground mb-1 block">Gravité</label>
        <div class="flex gap-1.5">
          @for (s of severityLevels; track s.value) {
            <button type="button" (click)="iForm.severity = s.value"
              [class]="'flex-1 py-2 text-xs rounded-lg border font-medium transition-all ' + (iForm.severity===s.value ? s.activeClass : 'border-border hover:border-red-300')">{{ s.label }}</button>
          }
        </div>
      </div>
      <div>
        <label class="text-xs font-medium text-muted-foreground mb-1 block">Heure</label>
        <input type="time" [(ngModel)]="iForm.occurredAt"
          class="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm focus:ring-2 focus:ring-red-400 focus:outline-none" />
      </div>
      <div>
        <label class="text-xs font-medium text-muted-foreground mb-1 block">Lieu</label>
        <input type="text" [(ngModel)]="iForm.location" placeholder="Ex: Salle de bain"
          class="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm focus:ring-2 focus:ring-red-400 focus:outline-none" />
      </div>
      <div>
        <label class="text-xs font-medium text-muted-foreground mb-1 block">Blessures</label>
        <input type="text" [(ngModel)]="iForm.injuryDetails" placeholder="Ex: Écorchure genou"
          class="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm focus:ring-2 focus:ring-red-400 focus:outline-none" />
      </div>
      <div class="md:col-span-2">
        <label class="text-xs font-medium text-muted-foreground mb-1 block">Actions prises</label>
        <input type="text" [(ngModel)]="iForm.actionTaken" placeholder="Ex: Appel médecin..."
          class="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm focus:ring-2 focus:ring-red-400 focus:outline-none" />
      </div>
    </div>
    <div class="flex gap-2 mt-4">
      <button z-button [disabled]="saving() || !iForm.description" (click)="saveIncident()">
        @if (saving()) { <z-icon zType="loader-2" class="mr-2 animate-spin" /> } Enregistrer
      </button>
      <button z-button zType="outline" (click)="cancelIncidentForm()">Annuler</button>
    </div>
  </z-card>
</ng-template>

<!-- ══ ERROR TOAST ══ -->
@if (errorMsg()) {
  <div class="fixed bottom-4 right-4 max-w-sm bg-destructive text-destructive-foreground px-4 py-3 rounded-xl shadow-xl flex items-center gap-2 z-50 animate-in slide-in-from-bottom-2">
    <z-icon zType="circle-x" class="h-4 w-4 shrink-0" />
    <span class="text-sm flex-1">{{ errorMsg() }}</span>
    <button (click)="errorMsg.set('')" class="hover:opacity-75"><z-icon zType="x" class="h-4 w-4" /></button>
  </div>
}
  `,
})
export class SuiviQuotidienComponent implements OnInit {
  @Input() keycloakId = '';
  @Output() goBack = new EventEmitter<void>();

  readonly today = todayIso();
  readonly pageSize = PAGE_SIZE;

  selectedDate = signal(todayIso());
  weekStart    = signal(getMondayOf(todayIso()));
  log          = signal<DailyLogResponse | null>(null);
  loading      = signal(false);
  saving       = signal(false);
  errorMsg     = signal('');
  logCache     = signal<Record<string, DailyLogResponse>>({});
  availableMeds = signal<AvailableMedication[]>([]);

  // ── Form visibility (signals) ──────────────────────────────────────────
  showNutritionForm = signal(false);
  showMedForm       = signal(false);
  showActivityForm  = signal(false);
  showIncidentForm  = signal(false);

  // ── Edit IDs (signals) ─────────────────────────────────────────────────
  editNutritionId = signal<number | null>(null);
  editMedId       = signal<number | null>(null);
  editActivityId  = signal<number | null>(null);
  editIncidentId  = signal<number | null>(null);

  // ── Form models ────────────────────────────────────────────────────────
  nForm: NutritionEntryRequest          = this.emptyNutrition();
  mForm: MedicationIntakeLogRequest     = this.emptyMed();
  aForm: ActivityEntryRequest           = this.emptyActivity();
  iForm: IncidentEntryRequest           = this.emptyIncident();

  // ── Search / sort — ALL SIGNALS so computed() tracks them ─────────────
  nSearch = signal('');  nSort = signal('');
  mSearch = signal('');  mSort = signal('');
  aSearch = signal('');  aSort = signal('');
  iSearch = signal('');  iSort = signal('');

  // ── Pagination — ALL SIGNALS ───────────────────────────────────────────
  nPage = signal(1);
  mPage = signal(1);
  aPage = signal(1);
  iPage = signal(1);

  // ── Week ───────────────────────────────────────────────────────────────
  weekDays      = computed(() => buildWeek(this.weekStart(), this.today));
  isCurrentWeek = computed(() => this.weekStart() === getMondayOf(this.today));
  weekMonthDisplay = computed(() => {
    const d = new Date(this.weekStart() + 'T12:00:00');
    return String(d.getMonth() + 1).padStart(2, '0') + '/' + d.getFullYear();
  });

  // ── Stats ──────────────────────────────────────────────────────────────
  prisTaken        = computed(() => this.log()?.medicationIntakes?.filter(m => m.status === 'PRIS').length ?? 0);
  totalActivityMin = computed(() => this.log()?.activityEntries?.reduce((s, a) => s + (a.durationMinutes ?? 0), 0) ?? 0);

  // ── Filtered + sorted (fully reactive) ────────────────────────────────
  nFilteredSorted = computed(() => {
    const q = this.nSearch().toLowerCase().trim();
    const sort = this.nSort();
    let list = [...(this.log()?.nutritionEntries ?? [])];
    if (q) list = list.filter(e =>
      e.description.toLowerCase().includes(q) ||
      e.mealType.toLowerCase().includes(q) ||
      (MEAL_LABELS[e.mealType]?.toLowerCase().includes(q))
    );
    if (sort === 'time')     list.sort((a, b) => (a.entryTime ?? '').localeCompare(b.entryTime ?? ''));
    if (sort === 'type')     list.sort((a, b) => a.mealType.localeCompare(b.mealType));
    if (sort === 'quantity') {
      const order: Record<string,number> = { RIEN:0, PEU:1, DEMI:2, COMPLET:3 };
      list.sort((a, b) => (order[b.quantity] ?? 0) - (order[a.quantity] ?? 0));
    }
    return list;
  });

  mFilteredSorted = computed(() => {
    const q = this.mSearch().toLowerCase().trim();
    const sort = this.mSort();
    let list = [...(this.log()?.medicationIntakes ?? [])];
    if (q) list = list.filter(e =>
      e.medicationName.toLowerCase().includes(q) ||
      (e.dosage?.toLowerCase().includes(q)) ||
      e.status.toLowerCase().includes(q)
    );
    if (sort === 'name')   list.sort((a, b) => a.medicationName.localeCompare(b.medicationName));
    if (sort === 'status') list.sort((a, b) => a.status.localeCompare(b.status));
    if (sort === 'time')   list.sort((a, b) => (a.takenAt ?? '').localeCompare(b.takenAt ?? ''));
    return list;
  });

  aFilteredSorted = computed(() => {
    const q = this.aSearch().toLowerCase().trim();
    const sort = this.aSort();
    let list = [...(this.log()?.activityEntries ?? [])];
    if (q) list = list.filter(e =>
      e.description.toLowerCase().includes(q) ||
      e.activityType.toLowerCase().includes(q)
    );
    if (sort === 'type')      list.sort((a, b) => a.activityType.localeCompare(b.activityType));
    if (sort === 'duration')  list.sort((a, b) => (b.durationMinutes ?? 0) - (a.durationMinutes ?? 0));
    if (sort === 'intensity') {
      const order: Record<string,number> = { ELEVE:3, MODERE:2, FAIBLE:1 };
      list.sort((a, b) => (order[b.intensity ?? ''] ?? 0) - (order[a.intensity ?? ''] ?? 0));
    }
    if (sort === 'time') list.sort((a, b) => (a.startTime ?? '').localeCompare(b.startTime ?? ''));
    return list;
  });

  iFilteredSorted = computed(() => {
    const q = this.iSearch().toLowerCase().trim();
    const sort = this.iSort();
    let list = [...(this.log()?.incidentEntries ?? [])];
    if (q) list = list.filter(e =>
      e.description.toLowerCase().includes(q) ||
      e.incidentType.toLowerCase().includes(q) ||
      (e.location?.toLowerCase().includes(q))
    );
    if (sort === 'severity') {
      const rank: Record<string,number> = { GRAVE:3, MODERE:2, LEGER:1 };
      list.sort((a, b) => (rank[b.severity] ?? 0) - (rank[a.severity] ?? 0));
    }
    if (sort === 'type') list.sort((a, b) => a.incidentType.localeCompare(b.incidentType));
    if (sort === 'time') list.sort((a, b) => (a.occurredAt ?? '').localeCompare(b.occurredAt ?? ''));
    return list;
  });

  // Reset page to 1 when filter changes
  private resetNPage = effect(() => { this.nFilteredSorted(); this.nPage.set(1); }, { allowSignalWrites: true });
  private resetMPage = effect(() => { this.mFilteredSorted(); this.mPage.set(1); }, { allowSignalWrites: true });
  private resetAPage = effect(() => { this.aFilteredSorted(); this.aPage.set(1); }, { allowSignalWrites: true });
  private resetIPage = effect(() => { this.iFilteredSorted(); this.iPage.set(1); }, { allowSignalWrites: true });

  // Paginated slices (computed signals)
  nPageItems = computed(() => this.nFilteredSorted().slice((this.nPage()-1)*PAGE_SIZE, this.nPage()*PAGE_SIZE));
  mPageItems = computed(() => this.mFilteredSorted().slice((this.mPage()-1)*PAGE_SIZE, this.mPage()*PAGE_SIZE));
  aPageItems = computed(() => this.aFilteredSorted().slice((this.aPage()-1)*PAGE_SIZE, this.aPage()*PAGE_SIZE));
  iPageItems = computed(() => this.iFilteredSorted().slice((this.iPage()-1)*PAGE_SIZE, this.iPage()*PAGE_SIZE));

  totalPages(total: number) { return Math.ceil(total / PAGE_SIZE); }
  min(a: number, b: number) { return Math.min(a, b); }

  changePage(section: string, p: number): void {
    if (section === 'n') this.nPage.set(p);
    if (section === 'm') this.mPage.set(p);
    if (section === 'a') this.aPage.set(p);
    if (section === 'i') this.iPage.set(p);
  }

  // ── Static option lists ────────────────────────────────────────────────
  intakeStatuses = Object.entries(INTAKE_STATUS_CONFIG).map(([value, cfg]) =>
    ({ value: value as IntakeStatus, ...cfg }));
  activityTypes = Object.entries(ACTIVITY_TYPE_ICONS).map(([value, icon]) =>
    ({ value: value as ActivityType, icon, label: value.charAt(0) + value.slice(1).toLowerCase() }));
  incidentTypes = Object.entries(INCIDENT_ICONS).map(([value, icon]) =>
    ({ value: value as IncidentType, icon, label: value.charAt(0) + value.slice(1).toLowerCase() }));
  severityLevels = [
    { value: 'LEGER' as SeverityLevel,  label: '🟡 Léger',  activeClass: 'bg-amber-500 text-white border-amber-500' },
    { value: 'MODERE' as SeverityLevel, label: '🟠 Modéré', activeClass: 'bg-orange-500 text-white border-orange-500' },
    { value: 'GRAVE' as SeverityLevel,  label: '🔴 Grave',  activeClass: 'bg-red-600 text-white border-red-600' },
  ];

  constructor(private readonly svc: DailyMonitoringService) {}

  ngOnInit(): void {
    this.loadLog();
    this.loadAvailableMeds();
  }

  // ── Week navigation ────────────────────────────────────────────────────
  prevWeek(): void { this.weekStart.set(addDaysIso(this.weekStart(), -7)); }
  nextWeek(): void { if (!this.isCurrentWeek()) this.weekStart.set(addDaysIso(this.weekStart(), 7)); }

  selectDay(iso: string): void {
    if (iso > this.today) return;
    this.selectedDate.set(iso);
    this.loadLog();
  }

  dayBtnClass(day: WeekDay): string {
    const base = 'flex flex-col items-center py-2 px-1 rounded-xl transition-all text-sm font-medium ';
    if (day.iso === this.selectedDate()) return base + 'bg-primary text-primary-foreground shadow-md';
    if (day.isToday)   return base + 'bg-primary/10 text-primary border border-primary/30';
    if (day.isFuture)  return base + 'opacity-25 cursor-not-allowed text-muted-foreground';
    return base + 'hover:bg-muted text-foreground cursor-pointer';
  }

  totalEntries(log: DailyLogResponse): string {
    const n = (log.nutritionEntries?.length ?? 0) + (log.medicationIntakes?.length ?? 0) +
              (log.activityEntries?.length ?? 0) + (log.incidentEntries?.length ?? 0);
    return n > 0 ? n + ' entr.' : '';
  }

  // ── Data loading ───────────────────────────────────────────────────────
  loadLog(): void {
    this.loading.set(true);
    this.svc.getOrCreateLogForDate(this.keycloakId, this.selectedDate()).subscribe({
      next: log => {
        this.log.set(log);
        this.logCache.update(c => ({ ...c, [log.logDate]: log }));
        this.loading.set(false);
      },
      error: () => { this.log.set(null); this.loading.set(false); },
    });
  }

  loadAvailableMeds(): void {
    this.svc.getAvailableMedications(this.keycloakId).subscribe({
      next: meds => this.availableMeds.set(meds),
      error: () => {},
    });
  }

  // ── Utility ────────────────────────────────────────────────────────────
  /** Extract input value from DOM event (for non-ngModel use) */
  getInputValue(event: Event): string {
    return (event.target as HTMLInputElement | HTMLSelectElement).value;
  }

  formatDateFr(iso: string): string {
    const d = new Date(iso + 'T12:00:00');
    const days   = ['Dimanche','Lundi','Mardi','Mercredi','Jeudi','Vendredi','Samedi'];
    const months = ['janvier','février','mars','avril','mai','juin','juillet','août','septembre','octobre','novembre','décembre'];
    return `${days[d.getDay()]} ${d.getDate()} ${months[d.getMonth()]} ${d.getFullYear()}`;
  }

  closeAllForms(): void {
    this.showNutritionForm.set(false);
    this.showMedForm.set(false);
    this.showActivityForm.set(false);
    this.showIncidentForm.set(false);
  }

  // ── Display helpers ────────────────────────────────────────────────────
  mealLabel(t: MealType): string        { return MEAL_LABELS[t] ?? t; }
  mealIcon(t: MealType): ZardIcon        { return MEAL_ICONS[t] ?? 'star'; }
  intakeStatusConfig(s: IntakeStatus)    { return INTAKE_STATUS_CONFIG[s] ?? INTAKE_STATUS_CONFIG['OUBLIE']; }
  severityConfig(s: SeverityLevel)       { return SEVERITY_CONFIG[s] ?? SEVERITY_CONFIG['LEGER']; }
  activityIcon(t: ActivityType): ZardIcon { return ACTIVITY_TYPE_ICONS[t] ?? 'star'; }
  incidentIcon(t: IncidentType): ZardIcon { return INCIDENT_ICONS[t] ?? 'shield'; }
  incidentBorderColor(s: SeverityLevel): string {
    return s === 'GRAVE' ? 'border-l-red-600' : s === 'MODERE' ? 'border-l-orange-500' : 'border-l-amber-400';
  }
  quantityBadge(q: QuantityLevel): string {
    return q === 'COMPLET' ? 'bg-emerald-100 text-emerald-700 border border-emerald-200'
         : q === 'DEMI'    ? 'bg-yellow-100 text-yellow-700 border border-yellow-200'
         : q === 'PEU'     ? 'bg-orange-100 text-orange-700 border border-orange-200'
         : 'bg-red-100 text-red-700 border border-red-200';
  }
  appetiteBadge(a: AppetiteLevel): string {
    return a === 'BON'   ? 'bg-emerald-100 text-emerald-700 border border-emerald-200'
         : a === 'MOYEN' ? 'bg-amber-100 text-amber-700 border border-amber-200'
         : 'bg-red-100 text-red-700 border border-red-200';
  }

  // ── Nutrition CRUD ─────────────────────────────────────────────────────
  editNutrition(e: NutritionEntryResponse): void {
    this.editNutritionId.set(e.id);
    this.nForm = { mealType: e.mealType, description: e.description, quantity: e.quantity,
      appetite: e.appetite, hydrationMl: e.hydrationMl, notes: e.notes, entryTime: e.entryTime };
    this.showNutritionForm.set(true);
  }
  cancelNutritionForm(): void {
    this.showNutritionForm.set(false); this.editNutritionId.set(null); this.nForm = this.emptyNutrition();
  }
  saveNutrition(): void {
    const logId = this.log()?.id; if (!logId) return;
    this.saving.set(true);
    const id = this.editNutritionId();
    (id ? this.svc.updateNutrition(logId, id, this.nForm) : this.svc.addNutrition(logId, this.nForm))
      .subscribe({
        next: () => { this.saving.set(false); this.cancelNutritionForm(); this.loadLog(); },
        error: () => { this.saving.set(false); this.errorMsg.set('Erreur lors de l\'enregistrement.'); },
      });
  }
  deleteNutrition(id: number): void {
    const logId = this.log()?.id; if (!logId) return;
    this.svc.deleteNutrition(logId, id).subscribe({
      next: () => this.loadLog(),
      error: () => this.errorMsg.set('Erreur lors de la suppression.'),
    });
  }

  // ── Medication CRUD ────────────────────────────────────────────────────
  editMedication(e: MedicationIntakeLogResponse): void {
    this.editMedId.set(e.id);
    this.mForm = { medicationId: e.medicationId, takenAt: e.takenAt, status: e.status, notes: e.notes };
    this.showMedForm.set(true);
  }
  cancelMedForm(): void {
    this.showMedForm.set(false); this.editMedId.set(null); this.mForm = this.emptyMed();
  }
  saveMedication(): void {
    const logId = this.log()?.id; if (!logId) return;
    this.saving.set(true);
    const id = this.editMedId();
    (id ? this.svc.updateMedicationIntake(logId, id, this.mForm) : this.svc.addMedicationIntake(logId, this.mForm))
      .subscribe({
        next: () => { this.saving.set(false); this.cancelMedForm(); this.loadLog(); },
        error: () => { this.saving.set(false); this.errorMsg.set('Erreur lors de l\'enregistrement.'); },
      });
  }
  deleteMedication(id: number): void {
    const logId = this.log()?.id; if (!logId) return;
    this.svc.deleteMedicationIntake(logId, id).subscribe({
      next: () => this.loadLog(),
      error: () => this.errorMsg.set('Erreur lors de la suppression.'),
    });
  }

  // ── Activity CRUD ──────────────────────────────────────────────────────
  editActivity(e: ActivityEntryResponse): void {
    this.editActivityId.set(e.id);
    this.aForm = { activityType: e.activityType, description: e.description,
      durationMinutes: e.durationMinutes, intensity: e.intensity, notes: e.notes, startTime: e.startTime };
    this.showActivityForm.set(true);
  }
  cancelActivityForm(): void {
    this.showActivityForm.set(false); this.editActivityId.set(null); this.aForm = this.emptyActivity();
  }
  saveActivity(): void {
    const logId = this.log()?.id; if (!logId) return;
    this.saving.set(true);
    const id = this.editActivityId();
    (id ? this.svc.updateActivity(logId, id, this.aForm) : this.svc.addActivity(logId, this.aForm))
      .subscribe({
        next: () => { this.saving.set(false); this.cancelActivityForm(); this.loadLog(); },
        error: () => { this.saving.set(false); this.errorMsg.set('Erreur lors de l\'enregistrement.'); },
      });
  }
  deleteActivity(id: number): void {
    const logId = this.log()?.id; if (!logId) return;
    this.svc.deleteActivity(logId, id).subscribe({
      next: () => this.loadLog(),
      error: () => this.errorMsg.set('Erreur lors de la suppression.'),
    });
  }

  // ── Incident CRUD ──────────────────────────────────────────────────────
  editIncident(e: IncidentEntryResponse): void {
    this.editIncidentId.set(e.id);
    this.iForm = { incidentType: e.incidentType, description: e.description, severity: e.severity,
      location: e.location, actionTaken: e.actionTaken, injuryDetails: e.injuryDetails, occurredAt: e.occurredAt };
    this.showIncidentForm.set(true);
  }
  cancelIncidentForm(): void {
    this.showIncidentForm.set(false); this.editIncidentId.set(null); this.iForm = this.emptyIncident();
  }
  saveIncident(): void {
    const logId = this.log()?.id; if (!logId) return;
    this.saving.set(true);
    const id = this.editIncidentId();
    (id ? this.svc.updateIncident(logId, id, this.iForm) : this.svc.addIncident(logId, this.iForm))
      .subscribe({
        next: () => { this.saving.set(false); this.cancelIncidentForm(); this.loadLog(); },
        error: () => { this.saving.set(false); this.errorMsg.set('Erreur lors de l\'enregistrement.'); },
      });
  }
  deleteIncident(id: number): void {
    const logId = this.log()?.id; if (!logId) return;
    this.svc.deleteIncident(logId, id).subscribe({
      next: () => this.loadLog(),
      error: () => this.errorMsg.set('Erreur lors de la suppression.'),
    });
  }

  // ── Empty forms ────────────────────────────────────────────────────────
  private emptyNutrition(): NutritionEntryRequest {
    return { mealType: 'BREAKFAST', description: '', quantity: 'COMPLET', appetite: 'BON' };
  }
  private emptyMed(): MedicationIntakeLogRequest {
    return { medicationId: undefined as any, takenAt: '', status: 'PRIS', notes: '' };
  }
  private emptyActivity(): ActivityEntryRequest {
    return { activityType: 'PHYSIQUE', description: '', intensity: 'MODERE' };
  }
  private emptyIncident(): IncidentEntryRequest {
    return { incidentType: 'CHUTE', description: '', severity: 'LEGER' };
  }
}
