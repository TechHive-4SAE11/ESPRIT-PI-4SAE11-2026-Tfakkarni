import {
  Component, Input, Output, EventEmitter, OnInit,
  signal, computed, effect
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { z } from 'zod';

import { ZardCardComponent }                        from '@/shared/components/card';
import { ZardIconComponent, type ZardIcon }        from '@/shared/components/icon';
import { ZardBadgeComponent }                      from '@/shared/components/badge';
import { ZardButtonComponent }                     from '@/shared/components/button';
import { ZardTabGroupComponent, ZardTabComponent } from '@/shared/components/tabs';
import { DailyMonitoringService }                  from '@/core/services/daily-monitoring.service';
import {
  DailyLogResponse, AvailableMedication,
  NutritionEntryRequest, NutritionEntryResponse,
  MealType, QuantityLevel, AppetiteLevel,
  MedicationIntakeLogRequest, MedicationIntakeLogResponse, IntakeStatus,
  ActivityEntryRequest, ActivityEntryResponse, ActivityType, IntensityLevel,
  IncidentEntryRequest, IncidentEntryResponse, IncidentType, SeverityLevel,
} from '@/core/models/daily-monitoring.model';

// ─── Date helpers ──────────────────────────────────────────────────────────────

function todayIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
}

function addDaysIso(iso: string, n: number): string {
  const d = new Date(iso + 'T12:00:00');
  d.setDate(d.getDate() + n);
  return d.toISOString().split('T')[0];
}

function getMondayOf(iso: string): string {
  const d   = new Date(iso + 'T12:00:00');
  const day = d.getDay();
  d.setDate(d.getDate() + (day === 0 ? -6 : 1 - day));
  return d.toISOString().split('T')[0];
}

interface WeekDay { iso: string; shortLabel: string; dayNum: number; isFuture: boolean; isToday: boolean; }
const DAY_FR = ['Dim','Lun','Mar','Mer','Jeu','Ven','Sam'];

function buildWeek(mondayIso: string, today: string): WeekDay[] {
  return Array.from({ length: 6 }, (_, i) => {
    const iso = addDaysIso(mondayIso, i);
    const d   = new Date(iso + 'T12:00:00');
    return { iso, shortLabel: DAY_FR[d.getDay()], dayNum: d.getDate(),
             isFuture: iso > today, isToday: iso === today };
  });
}

// ─── Display config ────────────────────────────────────────────────────────────

const MEAL_LABELS: Record<MealType, string>   = { BREAKFAST:'Petit-déjeuner', LUNCH:'Déjeuner', DINNER:'Dîner', SNACK:'Collation' };
const MEAL_ICONS:  Record<MealType, ZardIcon> = { BREAKFAST:'sun', LUNCH:'circle', DINNER:'moon', SNACK:'star' };

const STATUS_CFG: Record<IntakeStatus, { label: string; color: string; icon: ZardIcon }> = {
  PRIS:      { label:'Pris',      color:'bg-emerald-100 text-emerald-700 border border-emerald-200', icon:'check' },
  OUBLIE:    { label:'Oublié',    color:'bg-amber-100  text-amber-700  border border-amber-200',    icon:'triangle-alert' },
  REFUSE:    { label:'Refusé',    color:'bg-red-100    text-red-700    border border-red-200',       icon:'x' },
  EN_RETARD: { label:'En retard', color:'bg-blue-100   text-blue-700   border border-blue-200',     icon:'clock' },
};

const SEVERITY_CFG: Record<SeverityLevel, { label: string; color: string }> = {
  LEGER:  { label:'Léger',  color:'bg-amber-100  text-amber-700  border border-amber-200' },
  MODERE: { label:'Modéré', color:'bg-orange-100 text-orange-700 border border-orange-200' },
  GRAVE:  { label:'Grave',  color:'bg-red-100    text-red-700    border border-red-200' },
};

const ACT_ICONS: Record<ActivityType, ZardIcon> = {
  PHYSIQUE:'activity', COGNITIVE:'brain', SOCIALE:'users',
  HYGIENE:'heart', PROMENADE:'map-pin', AUTRE:'star',
};

const INC_ICONS: Record<IncidentType, ZardIcon> = {
  CHUTE:'triangle-alert', CONFUSION:'info', AGITATION:'zap',
  DEAMBULATION:'map-pin', CRISE:'bell', AUTRE:'shield',
};

const PAGE_SIZE = 5;

// ─── Zod Validation Schemas ────────────────────────────────────────────────────

const NutritionSchema = z.object({
  mealType: z.enum(['BREAKFAST', 'LUNCH', 'DINNER', 'SNACK']),
  description: z.string().min(1, 'La description est obligatoire').trim(),
  quantity: z.enum(['RIEN', 'PEU', 'DEMI', 'COMPLET']),
  appetite: z.enum(['FAIBLE', 'MOYEN', 'BON']),
  entryTime: z.string().optional(),
  hydrationMl: z.number().int().min(0, 'La valeur doit être ≥ 0').max(10000, 'Max 10 000 ml').optional(),
  notes: z.string().optional(),
});

const MedicationSchema = z.object({
  medicationId: z.number().or(z.string()).refine(v => v !== undefined && v !== '', 'Le médicament est obligatoire'),
  takenAt: z.string().min(1, 'L\'heure est obligatoire'),
  status: z.enum(['PRIS', 'OUBLIE', 'REFUSE', 'EN_RETARD']),
  notes: z.string().optional(),
});

const ActivitySchema = z.object({
  activityType: z.enum(['PHYSIQUE', 'COGNITIVE', 'SOCIALE', 'HYGIENE', 'PROMENADE', 'AUTRE']),
  description: z.string().min(1, 'La description est obligatoire').trim(),
  intensity: z.enum(['FAIBLE', 'MODERE', 'ELEVE']),
  durationMinutes: z.number().int().min(1, 'La durée doit être ≥ 1').max(1440, 'Max 1440 min').optional(),
  startTime: z.string().optional(),
});

const IncidentSchema = z.object({
  incidentType: z.enum(['CHUTE', 'CONFUSION', 'AGITATION', 'DEAMBULATION', 'CRISE', 'AUTRE']),
  description: z.string().min(1, 'La description est obligatoire').trim(),
  severity: z.enum(['LEGER', 'MODERE', 'GRAVE']),
  occurredAt: z.string().optional(),
  location: z.string().optional(),
  injuryDetails: z.string().optional(),
  actionTaken: z.string().optional(),
});

// ─── Validation helper ─────────────────────────────────────────────────────────

function isPositiveInt(v: string): boolean {
  return /^\d+$/.test(v.trim()) && parseInt(v, 10) > 0;
}

function isNumericOrEmpty(v: string): boolean {
  return v.trim() === '' || /^\d+(\.\d+)?$/.test(v.trim());
}

// ──────────────────────────────────────────────────────────────────────────────

@Component({
  selector: 'app-suivi-quotidien',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    ZardCardComponent, ZardIconComponent,
    ZardButtonComponent, ZardTabGroupComponent, ZardTabComponent,
  ],
  template: `
<!-- ══ HEADER ══ -->
<div class="flex items-center gap-3 mb-6">
  <button z-button zType="ghost" zSize="sm" (click)="goBack.emit()">
    <z-icon zType="arrow-left" />
  </button>
  <div>
    <h2 class="text-2xl font-bold tracking-tight">Suivi Quotidien</h2>
    <p class="text-sm text-muted-foreground">{{ formatDateFr(selectedDate()) }}</p>
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
        <button [disabled]="day.isFuture" (click)="selectDay(day.iso)" [class]="dayBtnClass(day)">
          <span class="text-[11px] font-semibold uppercase tracking-wide">{{ day.shortLabel }}</span>
          <span class="text-lg font-bold leading-tight">{{ day.dayNum }}</span>
          @if (day.isToday) { <span class="w-1.5 h-1.5 rounded-full bg-current"></span> }
          @if (logCache()[day.iso]; as c) {
            <span class="text-[9px] opacity-70">{{ totalEntries(c) }}</span>
          }
        </button>
      }
    </div>
    <button z-button zType="ghost" zSize="sm" [disabled]="isCurrentWeek()" (click)="nextWeek()">
      <z-icon zType="chevron-right" />
    </button>
  </div>
  <p class="text-center text-xs text-muted-foreground mt-2">
    Semaine du {{ weekDays()[0].dayNum }}/{{ weekMonthYear() }}
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
    <p class="text-xs text-muted-foreground">repas</p>
  </z-card>
  <z-card class="p-4 border-l-4 border-l-blue-400">
    <p class="text-xs text-muted-foreground mb-1">Médicaments</p>
    <p class="text-2xl font-bold">{{ prisTaken() }}<span class="text-sm font-normal text-muted-foreground">/{{ log()?.medicationIntakes?.length ?? 0 }}</span></p>
    <p class="text-xs text-muted-foreground">pris</p>
  </z-card>
  <z-card class="p-4 border-l-4 border-l-green-400">
    <p class="text-xs text-muted-foreground mb-1">Activités</p>
    <p class="text-2xl font-bold">{{ totalActMin() }}<span class="text-sm font-normal text-muted-foreground"> min</span></p>
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

  <!-- ─────────── VUE JOURNÉE (lecture seule) ─────────── -->
  <z-tab label="📋 Vue Journée">
    <div class="pt-4 space-y-6">
      <div class="flex items-center justify-between">
        <p class="text-sm text-muted-foreground">Résumé complet de la journée</p>
        <button z-button zType="outline" zSize="sm" (click)="openDayModal()">
          <z-icon zType="file" class="h-3.5 w-3.5 mr-1.5" />
          Télécharger PDF
        </button>
      </div>

      <!-- Alimentation -->
      <div>
        <h3 class="font-semibold text-base flex items-center gap-2 mb-3">
          <span class="p-1.5 rounded-lg bg-orange-100 text-orange-600"><z-icon zType="star" class="h-4 w-4" /></span>
          Alimentation <span class="text-xs font-normal text-muted-foreground">({{ log()?.nutritionEntries?.length ?? 0 }} repas)</span>
        </h3>
        @if ((log()?.nutritionEntries?.length ?? 0) > 0) {
          <div class="grid gap-2">
            @for (e of log()!.nutritionEntries; track e.id) {
              <div class="flex items-center gap-3 p-3 rounded-xl border bg-card">
                <div class="p-2 rounded-lg bg-orange-100 text-orange-600 shrink-0">
                  <z-icon [zType]="mealIcon(e.mealType)" class="h-4 w-4" />
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="font-medium text-sm">{{ mealLabel(e.mealType) }}</span>
                    @if (e.entryTime) { <span class="text-xs text-muted-foreground">{{ e.entryTime }}</span> }
                    <span [class]="'text-xs px-1.5 py-0.5 rounded-full font-medium ' + qtyBadge(e.quantity)">{{ e.quantity }}</span>
                  </div>
                  <p class="text-xs text-muted-foreground truncate mt-0.5">{{ e.description }}</p>
                  @if (e.hydrationMl) { <p class="text-xs text-blue-500 mt-0.5">💧 {{ e.hydrationMl }} ml</p> }
                </div>
              </div>
            }
          </div>
        } @else {
          <p class="text-center py-4 text-sm text-muted-foreground border border-dashed rounded-xl">Aucun repas enregistré</p>
        }
      </div>
      <div class="border-t border-dashed"></div>

      <!-- Médicaments -->
      <div>
        <h3 class="font-semibold text-base flex items-center gap-2 mb-3">
          <span class="p-1.5 rounded-lg bg-blue-100 text-blue-600"><z-icon zType="pill" class="h-4 w-4" /></span>
          Médicaments <span class="text-xs font-normal text-muted-foreground">({{ prisTaken() }}/{{ log()?.medicationIntakes?.length ?? 0 }} pris)</span>
        </h3>
        @if ((log()?.medicationIntakes?.length ?? 0) > 0) {
          <div class="grid gap-2">
            @for (e of log()!.medicationIntakes; track e.id) {
              <div class="flex items-center gap-3 p-3 rounded-xl border bg-card">
                <div class="p-2 rounded-lg shrink-0"
                  [class]="e.status==='PRIS'?'bg-emerald-100 text-emerald-600':e.status==='OUBLIE'?'bg-amber-100 text-amber-600':e.status==='REFUSE'?'bg-red-100 text-red-600':'bg-blue-100 text-blue-600'">
                  <z-icon [zType]="statusCfg(e.status).icon" class="h-4 w-4" />
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="font-medium text-sm">{{ e.medicationName }}</span>
                    @if (e.dosage) { <span class="text-xs text-muted-foreground">{{ e.dosage }}</span> }
                    <span [class]="'text-xs px-1.5 py-0.5 rounded-full font-medium ' + statusCfg(e.status).color">{{ statusCfg(e.status).label }}</span>
                  </div>
                  @if (e.takenAt) { <p class="text-xs text-muted-foreground mt-0.5">Pris à {{ e.takenAt }}</p> }
                </div>
              </div>
            }
          </div>
        } @else {
          <p class="text-center py-4 text-sm text-muted-foreground border border-dashed rounded-xl">Aucune prise médicamenteuse</p>
        }
      </div>
      <div class="border-t border-dashed"></div>

      <!-- Activités -->
      <div>
        <h3 class="font-semibold text-base flex items-center gap-2 mb-3">
          <span class="p-1.5 rounded-lg bg-green-100 text-green-600"><z-icon zType="activity" class="h-4 w-4" /></span>
          Activités <span class="text-xs font-normal text-muted-foreground">({{ totalActMin() }} min)</span>
        </h3>
        @if ((log()?.activityEntries?.length ?? 0) > 0) {
          <div class="grid gap-2">
            @for (e of log()!.activityEntries; track e.id) {
              <div class="flex items-center gap-3 p-3 rounded-xl border bg-card">
                <div class="p-2 rounded-lg bg-green-100 text-green-600 shrink-0">
                  <z-icon [zType]="actIcon(e.activityType)" class="h-4 w-4" />
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="font-medium text-sm">{{ e.description }}</span>
                    <span class="text-xs px-1.5 py-0.5 rounded-full bg-green-100 text-green-700 border border-green-200 font-medium">{{ e.activityType }}</span>
                  </div>
                  <div class="flex gap-3 mt-0.5 text-xs text-muted-foreground">
                    @if (e.durationMinutes) { <span>{{ e.durationMinutes }} min</span> }
                    @if (e.startTime) { <span>Début : {{ e.startTime }}</span> }
                  </div>
                </div>
              </div>
            }
          </div>
        } @else {
          <p class="text-center py-4 text-sm text-muted-foreground border border-dashed rounded-xl">Aucune activité enregistrée</p>
        }
      </div>
      <div class="border-t border-dashed"></div>

      <!-- Incidents -->
      <div>
        <h3 class="font-semibold text-base flex items-center gap-2 mb-3">
          <span class="p-1.5 rounded-lg bg-red-100 text-red-600"><z-icon zType="triangle-alert" class="h-4 w-4" /></span>
          Incidents
        </h3>
        @if ((log()?.incidentEntries?.length ?? 0) > 0) {
          <div class="grid gap-2">
            @for (e of log()!.incidentEntries; track e.id) {
              <div class="flex items-center gap-3 p-3 rounded-xl border-l-4 border bg-card" [class]="incBorder(e.severity)">
                <div class="p-2 rounded-lg bg-red-100 text-red-600 shrink-0">
                  <z-icon [zType]="incIcon(e.incidentType)" class="h-4 w-4" />
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="font-medium text-sm">{{ e.incidentType }}</span>
                    <span [class]="'text-xs px-1.5 py-0.5 rounded-full font-medium ' + sevCfg(e.severity).color">{{ sevCfg(e.severity).label }}</span>
                  </div>
                  <p class="text-xs text-muted-foreground truncate mt-0.5">{{ e.description }}</p>
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
    </div>
  </z-tab>

  <!-- ─────────── ALIMENTATION ─────────── -->
  <z-tab label="🍽️ Alimentation">
    <div class="pt-4 space-y-3">
      <div class="flex gap-2 flex-wrap">
        <input [value]="nSearch()" (input)="nSearch.set(getVal($event))" placeholder="Rechercher un repas..."
          class="flex-1 min-w-[150px] px-3 py-2 text-sm border border-border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-primary" />
        <select [value]="nSort()" (change)="nSort.set(getVal($event))"
          class="px-3 py-2 text-sm border border-border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-primary">
          <option value="">Trier par...</option>
          <option value="time">Heure</option>
          <option value="type">Type repas</option>
          <option value="quantity">Quantité</option>
        </select>
        @if (!readOnly) {
          <button z-button zType="outline" zSize="sm" (click)="openNutritionForm()">
            <z-icon zType="plus" class="h-3.5 w-3.5 mr-1" /> Ajouter
          </button>
        }
      </div>
      @if (nSearch()) {
        <p class="text-xs text-muted-foreground">{{ nFiltered().length }} résultat(s)
          <button class="ml-1 text-primary underline" (click)="nSearch.set('')">Effacer</button>
        </p>
      }
      @for (e of nPageItems(); track e.id) {
        <z-card class="p-4">
          <div class="flex items-start justify-between gap-3">
            <div class="flex gap-3">
              <div class="p-2 rounded-lg bg-orange-100 text-orange-600 shrink-0">
                <z-icon [zType]="mealIcon(e.mealType)" class="h-4 w-4" />
              </div>
              <div>
                <div class="flex items-center gap-2 flex-wrap">
                  <span class="font-semibold text-sm">{{ mealLabel(e.mealType) }}</span>
                  @if (e.entryTime) { <span class="text-xs text-muted-foreground">{{ e.entryTime }}</span> }
                  <span [class]="'text-xs px-2 py-0.5 rounded-full font-medium ' + qtyBadge(e.quantity)">{{ e.quantity }}</span>
                </div>
                <p class="text-sm text-muted-foreground mt-1">{{ e.description }}</p>
                @if (e.hydrationMl) { <p class="text-xs text-blue-600 mt-1">💧 {{ e.hydrationMl }} ml</p> }
                @if (e.notes) { <p class="text-xs italic text-muted-foreground mt-1">{{ e.notes }}</p> }
              </div>
            </div>
            @if (!readOnly) {
              <div class="flex gap-1 shrink-0">
                <button z-button zType="ghost" zSize="sm" (click)="editNutrition(e)"><z-icon zType="edit" class="h-3.5 w-3.5" /></button>
                <button z-button zType="ghost" zSize="sm" class="text-destructive" (click)="deleteNutrition(e.id)"><z-icon zType="trash-2" class="h-3.5 w-3.5" /></button>
              </div>
            }
          </div>
        </z-card>
      }
      @if (nFiltered().length === 0) {
        <div class="text-center py-10 text-muted-foreground">
          <z-icon zType="star" class="mx-auto h-10 w-10 mb-3 opacity-30" />
          <p class="text-sm">{{ nSearch() ? 'Aucun résultat.' : 'Aucun repas enregistré.' }}</p>
        </div>
      }
      <ng-container *ngTemplateOutlet="pagTpl; context:{total:nFiltered().length, page:nPage(), tab:'n'}" />
    </div>
  </z-tab>

  <!-- ─────────── MÉDICAMENTS ─────────── -->
  <z-tab label="💊 Médicaments">
    <div class="pt-4 space-y-3">
      <div class="flex gap-2 flex-wrap">
        <input [value]="mSearch()" (input)="mSearch.set(getVal($event))" placeholder="Rechercher un médicament..."
          class="flex-1 min-w-[150px] px-3 py-2 text-sm border border-border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-primary" />
        <select [value]="mSort()" (change)="mSort.set(getVal($event))"
          class="px-3 py-2 text-sm border border-border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-primary">
          <option value="">Trier par...</option>
          <option value="name">Nom</option>
          <option value="status">Statut</option>
          <option value="time">Heure</option>
        </select>
        @if (!readOnly) {
          <button z-button zType="outline" zSize="sm" (click)="openMedForm()">
            <z-icon zType="plus" class="h-3.5 w-3.5 mr-1" /> Ajouter
          </button>
        }
      </div>
      @if (mSearch()) {
        <p class="text-xs text-muted-foreground">{{ mFiltered().length }} résultat(s)
          <button class="ml-1 text-primary underline" (click)="mSearch.set('')">Effacer</button>
        </p>
      }
      @for (e of mPageItems(); track e.id) {
        <z-card class="p-4">
          <div class="flex items-start justify-between gap-3">
            <div class="flex gap-3">
              <div class="p-2 rounded-lg bg-blue-100 text-blue-600 shrink-0">
                <z-icon [zType]="statusCfg(e.status).icon" class="h-4 w-4" />
              </div>
              <div>
                <div class="flex items-center gap-2 flex-wrap">
                  <span class="font-semibold text-sm">{{ e.medicationName }}</span>
                  @if (e.dosage) { <span class="text-xs text-muted-foreground">{{ e.dosage }}</span> }
                  <span [class]="'text-xs px-2 py-0.5 rounded-full font-medium ' + statusCfg(e.status).color">{{ statusCfg(e.status).label }}</span>
                </div>
                @if (e.takenAt) { <p class="text-xs text-muted-foreground mt-1">Pris à {{ e.takenAt }}</p> }
                @if (e.notes) { <p class="text-xs italic text-muted-foreground mt-1">{{ e.notes }}</p> }
              </div>
            </div>
            @if (!readOnly) {
              <div class="flex gap-1 shrink-0">
                <button z-button zType="ghost" zSize="sm" (click)="editMedication(e)"><z-icon zType="edit" class="h-3.5 w-3.5" /></button>
                <button z-button zType="ghost" zSize="sm" class="text-destructive" (click)="deleteMedication(e.id)"><z-icon zType="trash-2" class="h-3.5 w-3.5" /></button>
              </div>
            }
          </div>
        </z-card>
      }
      @if (mFiltered().length === 0) {
        <div class="text-center py-10 text-muted-foreground">
          <z-icon zType="pill" class="mx-auto h-10 w-10 mb-3 opacity-30" />
          <p class="text-sm">{{ mSearch() ? 'Aucun résultat.' : 'Aucune prise médicamenteuse.' }}</p>
        </div>
      }
      <ng-container *ngTemplateOutlet="pagTpl; context:{total:mFiltered().length, page:mPage(), tab:'m'}" />
    </div>
  </z-tab>

  <!-- ─────────── ACTIVITÉS ─────────── -->
  <z-tab label="🏃 Activités">
    <div class="pt-4 space-y-3">
      <div class="flex gap-2 flex-wrap">
        <input [value]="aSearch()" (input)="aSearch.set(getVal($event))" placeholder="Rechercher une activité..."
          class="flex-1 min-w-[150px] px-3 py-2 text-sm border border-border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-primary" />
        <select [value]="aSort()" (change)="aSort.set(getVal($event))"
          class="px-3 py-2 text-sm border border-border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-primary">
          <option value="">Trier par...</option>
          <option value="type">Type</option>
          <option value="duration">Durée ↓</option>
          <option value="intensity">Intensité</option>
          <option value="time">Heure</option>
        </select>
        @if (!readOnly) {
          <button z-button zType="outline" zSize="sm" (click)="openActivityForm()">
            <z-icon zType="plus" class="h-3.5 w-3.5 mr-1" /> Ajouter
          </button>
        }
      </div>
      @if (aSearch()) {
        <p class="text-xs text-muted-foreground">{{ aFiltered().length }} résultat(s)
          <button class="ml-1 text-primary underline" (click)="aSearch.set('')">Effacer</button>
        </p>
      }
      @for (e of aPageItems(); track e.id) {
        <z-card class="p-4">
          <div class="flex items-start justify-between gap-3">
            <div class="flex gap-3">
              <div class="p-2 rounded-lg bg-green-100 text-green-600 shrink-0">
                <z-icon [zType]="actIcon(e.activityType)" class="h-4 w-4" />
              </div>
              <div>
                <div class="flex items-center gap-2 flex-wrap">
                  <span class="font-semibold text-sm">{{ e.description }}</span>
                  <span class="text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-700 border border-green-200 font-medium">{{ e.activityType }}</span>
                  @if (e.intensity) { <span class="text-xs text-muted-foreground">{{ e.intensity }}</span> }
                </div>
                <div class="flex gap-3 mt-1 text-xs text-muted-foreground">
                  @if (e.durationMinutes) { <span>{{ e.durationMinutes }} min</span> }
                  @if (e.startTime) { <span>Début : {{ e.startTime }}</span> }
                </div>
              </div>
            </div>
            @if (!readOnly) {
              <div class="flex gap-1 shrink-0">
                <button z-button zType="ghost" zSize="sm" (click)="editActivity(e)"><z-icon zType="edit" class="h-3.5 w-3.5" /></button>
                <button z-button zType="ghost" zSize="sm" class="text-destructive" (click)="deleteActivity(e.id)"><z-icon zType="trash-2" class="h-3.5 w-3.5" /></button>
              </div>
            }
          </div>
        </z-card>
      }
      @if (aFiltered().length === 0) {
        <div class="text-center py-10 text-muted-foreground">
          <z-icon zType="activity" class="mx-auto h-10 w-10 mb-3 opacity-30" />
          <p class="text-sm">{{ aSearch() ? 'Aucun résultat.' : 'Aucune activité enregistrée.' }}</p>
        </div>
      }
      <ng-container *ngTemplateOutlet="pagTpl; context:{total:aFiltered().length, page:aPage(), tab:'a'}" />
    </div>
  </z-tab>

  <!-- ─────────── INCIDENTS ─────────── -->
  <z-tab label="⚠️ Incidents">
    <div class="pt-4 space-y-3">
      <div class="flex gap-2 flex-wrap">
        <input [value]="iSearch()" (input)="iSearch.set(getVal($event))" placeholder="Rechercher un incident..."
          class="flex-1 min-w-[150px] px-3 py-2 text-sm border border-border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-primary" />
        <select [value]="iSort()" (change)="iSort.set(getVal($event))"
          class="px-3 py-2 text-sm border border-border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-primary">
          <option value="">Trier par...</option>
          <option value="severity">Gravité ↓</option>
          <option value="type">Type</option>
          <option value="time">Heure</option>
        </select>
        @if (!readOnly) {
          <button z-button zType="outline" zSize="sm"
            class="border-red-200 text-red-600 hover:bg-red-50" (click)="openIncidentForm()">
            <z-icon zType="triangle-alert" class="h-3.5 w-3.5 mr-1" /> Signaler
          </button>
        }
      </div>
      @if (iSearch()) {
        <p class="text-xs text-muted-foreground">{{ iFiltered().length }} résultat(s)
          <button class="ml-1 text-primary underline" (click)="iSearch.set('')">Effacer</button>
        </p>
      }
      @for (e of iPageItems(); track e.id) {
        <z-card class="p-4 border-l-4" [class]="incBorder(e.severity)">
          <div class="flex items-start justify-between gap-3">
            <div class="flex gap-3">
              <div class="p-2 rounded-lg bg-red-100 text-red-600 shrink-0">
                <z-icon [zType]="incIcon(e.incidentType)" class="h-4 w-4" />
              </div>
              <div>
                <div class="flex items-center gap-2 flex-wrap">
                  <span class="font-semibold text-sm">{{ e.incidentType }}</span>
                  <span [class]="'text-xs px-2 py-0.5 rounded-full font-medium ' + sevCfg(e.severity).color">{{ sevCfg(e.severity).label }}</span>
                  @if (e.occurredAt) { <span class="text-xs text-muted-foreground">{{ e.occurredAt }}</span> }
                </div>
                <p class="text-sm text-muted-foreground mt-1">{{ e.description }}</p>
                @if (e.location) { <p class="text-xs text-muted-foreground mt-1">📍 {{ e.location }}</p> }
                @if (e.actionTaken) { <p class="text-xs text-green-700 mt-1">✓ {{ e.actionTaken }}</p> }
              </div>
            </div>
            @if (!readOnly) {
              <div class="flex gap-1 shrink-0">
                <button z-button zType="ghost" zSize="sm" (click)="editIncident(e)"><z-icon zType="edit" class="h-3.5 w-3.5" /></button>
                <button z-button zType="ghost" zSize="sm" class="text-destructive" (click)="deleteIncident(e.id)"><z-icon zType="trash-2" class="h-3.5 w-3.5" /></button>
              </div>
            }
          </div>
        </z-card>
      }
      @if (iFiltered().length === 0) {
        <div class="flex items-center justify-center gap-2 py-10 text-green-600 text-sm">
          <z-icon zType="shield" class="h-5 w-5" />
          {{ iSearch() ? 'Aucun résultat.' : 'Aucun incident — journée calme !' }}
        </div>
      }
      <ng-container *ngTemplateOutlet="pagTpl; context:{total:iFiltered().length, page:iPage(), tab:'i'}" />
    </div>
  </z-tab>

</z-tab-group>
} <!-- /loading -->

<!-- ══ PAGINATION TEMPLATE ══ -->
<ng-template #pagTpl let-total="total" let-page="page" let-tab="tab">
  @if (total > pageSize) {
    <div class="flex items-center justify-between pt-3 border-t border-border/50">
      <span class="text-xs text-muted-foreground">
        {{ (page-1)*pageSize+1 }}–{{ min(page*pageSize, total) }} sur {{ total }}
      </span>
      <div class="flex items-center gap-1">
        <button z-button zType="outline" zSize="sm" [disabled]="page<=1" (click)="changePage(tab, page-1)">
          <z-icon zType="chevron-left" class="h-3.5 w-3.5" />
        </button>
        <span class="px-2 text-xs font-medium">{{ page }}/{{ totPages(total) }}</span>
        <button z-button zType="outline" zSize="sm" [disabled]="page>=totPages(total)" (click)="changePage(tab, page+1)">
          <z-icon zType="chevron-right" class="h-3.5 w-3.5" />
        </button>
      </div>
    </div>
  }
</ng-template>

<!-- ══════════════════════════════════════════════════════ -->
<!-- ══ MODAL OVERLAY ══                                   -->
<!-- ══════════════════════════════════════════════════════ -->

@if (activeModal() && (activeModal() === 'day' || !readOnly)) {
  <div class="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4 bg-black/50 backdrop-blur-sm"
    (click)="closeModal()">

    <div class="relative w-full sm:max-w-xl max-h-[92dvh] sm:max-h-[88vh] overflow-hidden
                rounded-t-3xl sm:rounded-2xl bg-background shadow-2xl ring-1 ring-border/60
                flex flex-col"
      (click)="$event.stopPropagation()">

      <!-- Drag handle mobile -->
      <div class="sm:hidden flex justify-center pt-3 pb-1 shrink-0">
        <div class="w-10 h-1 rounded-full bg-border/70"></div>
      </div>

      <!-- Header -->
      <div class="flex items-center justify-between px-6 py-4 border-b border-border shrink-0">
        <div class="flex items-center gap-2">
          <z-icon [zType]="modalIcon()" class="h-5 w-5 text-primary" />
          <h3 class="font-semibold text-base">{{ modalTitle() }}</h3>
        </div>
        <button z-button zType="ghost" zSize="sm" class="rounded-full" (click)="closeModal()">
          <z-icon zType="x" class="h-4 w-4" />
        </button>
      </div>

      <!-- Body scrollable -->
      <div class="flex-1 overflow-y-auto px-6 py-5">

        <!-- ═════════ FORMULAIRE NUTRITION ═════════ -->
        @if (activeModal() === 'nutrition') {
          <div class="space-y-4">

            <!-- Type repas + Heure -->
            <div class="grid gap-4 md:grid-cols-2">
              <div>
                <label class="lbl">Type de repas <span class="req">*</span></label>
                <select [(ngModel)]="nForm.mealType" class="finput">
                  <option value="BREAKFAST">🌅 Petit-déjeuner</option>
                  <option value="LUNCH">☀️ Déjeuner</option>
                  <option value="DINNER">🌙 Dîner</option>
                  <option value="SNACK">⭐ Collation</option>
                </select>
              </div>
              <div>
                <label class="lbl">Heure du repas <span class="req">*</span></label>
                <input type="time" [(ngModel)]="nForm.entryTime"
                  class="finput" [class.finput-err]="formSubmitted() && !nForm.entryTime" />
                @if (formSubmitted() && !nForm.entryTime) {
                  <p class="ferr">Ce champ est obligatoire</p>
                }
              </div>
            </div>

            <!-- Description -->
            <div>
              <label class="lbl">Aliments consommés <span class="req">*</span></label>
              <input type="text" [(ngModel)]="nForm.description"
                placeholder="Ex: Soupe de légumes, pain complet..."
                class="finput" [class.finput-err]="formSubmitted() && !nForm.description.trim()" />
              @if (formSubmitted() && !nForm.description.trim()) {
                <p class="ferr">Ce champ est obligatoire</p>
              }
            </div>

            <!-- Quantité -->
            <div>
              <label class="lbl">Quantité consommée <span class="req">*</span></label>
              <div class="flex gap-1.5 flex-wrap mt-1">
                @for (o of [['COMPLET','✅ Tout'],['DEMI','½ Moitié'],['PEU','🔸 Peu'],['RIEN','❌ Rien']]; track o[0]) {
                  <button type="button" (click)="nForm.quantity = $any(o[0])"
                    [class]="'chip ' + (nForm.quantity===o[0] ? 'chip-on' : 'chip-off')">{{ o[1] }}</button>
                }
              </div>
            </div>

            <!-- Appétit -->
            <div>
              <label class="lbl">Appétit <span class="req">*</span></label>
              <div class="flex gap-2 mt-1">
                @for (o of [['BON','😊 Bon'],['MOYEN','😐 Moyen'],['FAIBLE','😟 Faible']]; track o[0]) {
                  <button type="button" (click)="nForm.appetite = $any(o[0])"
                    [class]="'flex-1 chip ' + (nForm.appetite===o[0] ? 'chip-on' : 'chip-off')">{{ o[1] }}</button>
                }
              </div>
            </div>

            <!-- Hydratation (numérique avec validation) -->
            <div class="grid gap-4 md:grid-cols-2">
              <div>
                <label class="lbl">💧 Hydratation (ml) <span class="req">*</span></label>
                <input type="text" inputmode="numeric"
                  [value]="nHydroRaw()"
                  (input)="nHydroRaw.set(getVal($event))"
                  placeholder="Ex: 200"
                  class="finput"
                  [class.finput-err]="formSubmitted() && !!nHydroErr()" />
                @if (formSubmitted() && nHydroErr()) {
                  <p class="ferr">{{ nHydroErr() }}</p>
                }
              </div>
              <div>
                <label class="lbl">Notes</label>
                <input type="text" [(ngModel)]="nForm.notes"
                  placeholder="Observations..." class="finput" />
              </div>
            </div>

          </div>
        }

        <!-- ═════════ FORMULAIRE MÉDICAMENT ═════════ -->
        @if (activeModal() === 'medication') {
          <div class="space-y-4">

            @if (availableMeds().length === 0) {
              <div class="flex items-start gap-2 p-3 rounded-xl bg-amber-50 border border-amber-200 text-amber-700 text-sm">
                <z-icon zType="triangle-alert" class="h-4 w-4 mt-0.5 shrink-0" />
                <span>Aucun médicament prescrit trouvé pour ce patient.</span>
              </div>
            }

            <!-- Médicament -->
            <div>
              <label class="lbl">Médicament prescrit <span class="req">*</span></label>
              <select [(ngModel)]="mForm.medicationId" class="finput"
                [class.finput-err]="formSubmitted() && !mForm.medicationId">
                <option [ngValue]="undefined">-- Choisir un médicament --</option>
                @for (med of availableMeds(); track med.id) {
                  <option [ngValue]="med.id">
                    {{ med.medicationName }}{{ med.dosage ? ' – '+med.dosage : '' }}{{ med.frequency ? ' ('+med.frequency+')' : '' }}
                  </option>
                }
              </select>
              @if (formSubmitted() && !mForm.medicationId) {
                <p class="ferr">Veuillez sélectionner un médicament</p>
              }
            </div>

            <!-- Heure + Statut -->
            <div class="grid gap-4 md:grid-cols-2">
              <div>
                <label class="lbl">Heure de prise <span class="req">*</span></label>
                <input type="time" [(ngModel)]="mForm.takenAt" class="finput"
                  [class.finput-err]="formSubmitted() && !mForm.takenAt" />
                @if (formSubmitted() && !mForm.takenAt) {
                  <p class="ferr">Ce champ est obligatoire</p>
                }
              </div>
              <div>
                <label class="lbl">Statut <span class="req">*</span></label>
                <div class="grid grid-cols-2 gap-1.5 mt-1">
                  @for (s of intakeStatusList; track s.value) {
                    <button type="button" (click)="mForm.status = s.value"
                      [class]="'chip flex items-center gap-1.5 ' + (mForm.status===s.value ? 'chip-on' : 'chip-off')">
                      <z-icon [zType]="s.icon" class="h-3.5 w-3.5" />{{ s.label }}
                    </button>
                  }
                </div>
              </div>
            </div>

            <!-- Notes -->
            <div>
              <label class="lbl">Notes</label>
              <input type="text" [(ngModel)]="mForm.notes"
                placeholder="Observations..." class="finput" />
            </div>

          </div>
        }

        <!-- ═════════ FORMULAIRE ACTIVITÉ ═════════ -->
        @if (activeModal() === 'activity') {
          <div class="space-y-4">

            <!-- Type -->
            <div>
              <label class="lbl">Type d'activité <span class="req">*</span></label>
              <div class="grid grid-cols-3 md:grid-cols-6 gap-1.5 mt-1">
                @for (t of actTypeList; track t.value) {
                  <button type="button" (click)="aForm.activityType = t.value"
                    [class]="'py-2 px-1 text-xs rounded-xl border font-medium flex flex-col items-center gap-1 transition-all ' +
                    (aForm.activityType===t.value
                      ? 'bg-primary text-primary-foreground border-primary shadow-sm'
                      : 'border-border hover:border-primary/40')">
                    <z-icon [zType]="t.icon" class="h-4 w-4" />{{ t.label }}
                  </button>
                }
              </div>
            </div>

            <!-- Description -->
            <div>
              <label class="lbl">Description <span class="req">*</span></label>
              <input type="text" [(ngModel)]="aForm.description"
                placeholder="Ex: Marche dans le jardin..."
                class="finput" [class.finput-err]="formSubmitted() && !aForm.description.trim()" />
              @if (formSubmitted() && !aForm.description.trim()) {
                <p class="ferr">Ce champ est obligatoire</p>
              }
            </div>

            <!-- Durée + Heure -->
            <div class="grid gap-4 md:grid-cols-2">
              <div>
                <label class="lbl">Durée (minutes) <span class="req">*</span></label>
                <input type="text" inputmode="numeric"
                  [value]="aDurRaw()"
                  (input)="aDurRaw.set(getVal($event))"
                  placeholder="Ex: 30"
                  class="finput"
                  [class.finput-err]="formSubmitted() && !!aDurErr()" />
                @if (formSubmitted() && aDurErr()) {
                  <p class="ferr">{{ aDurErr() }}</p>
                }
              </div>
              <div>
                <label class="lbl">Heure de début <span class="req">*</span></label>
                <input type="time" [(ngModel)]="aForm.startTime" class="finput"
                  [class.finput-err]="formSubmitted() && !aForm.startTime" />
                @if (formSubmitted() && !aForm.startTime) {
                  <p class="ferr">Ce champ est obligatoire</p>
                }
              </div>
            </div>

            <!-- Intensité -->
            <div>
              <label class="lbl">Intensité</label>
              <div class="flex gap-2 mt-1">
                @for (o of [['FAIBLE','🟢 Faible'],['MODERE','🟡 Modérée'],['ELEVE','🔴 Élevée']]; track o[0]) {
                  <button type="button" (click)="aForm.intensity = $any(o[0])"
                    [class]="'flex-1 chip ' + (aForm.intensity===o[0] ? 'chip-on' : 'chip-off')">{{ o[1] }}</button>
                }
              </div>
            </div>

            <!-- Notes -->
            <div>
              <label class="lbl">Notes</label>
              <input type="text" [(ngModel)]="aForm.notes"
                placeholder="Observations..." class="finput" />
            </div>

          </div>
        }

        <!-- ═════════ FORMULAIRE INCIDENT ═════════ -->
        @if (activeModal() === 'incident') {
          <div class="space-y-4">

            <!-- Type -->
            <div>
              <label class="lbl">Type d'incident <span class="req">*</span></label>
              <div class="grid grid-cols-3 md:grid-cols-6 gap-1.5 mt-1">
                @for (t of incTypeList; track t.value) {
                  <button type="button" (click)="iForm.incidentType = t.value"
                    [class]="'py-2 px-1 text-xs rounded-xl border font-medium flex flex-col items-center gap-1 transition-all ' +
                    (iForm.incidentType===t.value
                      ? 'bg-red-600 text-white border-red-600 shadow-sm'
                      : 'border-border hover:border-red-300')">
                    <z-icon [zType]="t.icon" class="h-4 w-4" />{{ t.label }}
                  </button>
                }
              </div>
            </div>

            <!-- Description -->
            <div>
              <label class="lbl">Description <span class="req">*</span></label>
              <input type="text" [(ngModel)]="iForm.description"
                placeholder="Décrivez l'incident..."
                class="finput" [class.finput-err]="formSubmitted() && !iForm.description.trim()" />
              @if (formSubmitted() && !iForm.description.trim()) {
                <p class="ferr">Ce champ est obligatoire</p>
              }
            </div>

            <!-- Gravité + Heure -->
            <div class="grid gap-4 md:grid-cols-2">
              <div>
                <label class="lbl">Gravité</label>
                <div class="flex gap-1.5 mt-1">
                  @for (s of sevList; track s.value) {
                    <button type="button" (click)="iForm.severity = s.value"
                      [class]="'flex-1 chip ' + (iForm.severity===s.value ? s.activeClass : 'chip-off')">{{ s.label }}</button>
                  }
                </div>
              </div>
              <div>
                <label class="lbl">Heure <span class="req">*</span></label>
                <input type="time" [(ngModel)]="iForm.occurredAt" class="finput"
                  [class.finput-err]="formSubmitted() && !iForm.occurredAt" />
                @if (formSubmitted() && !iForm.occurredAt) {
                  <p class="ferr">Ce champ est obligatoire</p>
                }
              </div>
            </div>

            <!-- Lieu + Blessures -->
            <div class="grid gap-4 md:grid-cols-2">
              <div>
                <label class="lbl">Lieu <span class="req">*</span></label>
                <input type="text" [(ngModel)]="iForm.location"
                  placeholder="Ex: Salle de bain" class="finput"
                  [class.finput-err]="formSubmitted() && !iForm.location?.trim()" />
                @if (formSubmitted() && !iForm.location?.trim()) {
                  <p class="ferr">Ce champ est obligatoire</p>
                }
              </div>
              <div>
                <label class="lbl">Blessures</label>
                <input type="text" [(ngModel)]="iForm.injuryDetails"
                  placeholder="Ex: Écorchure genou" class="finput" />
              </div>
            </div>

            <!-- Action prise -->
            <div>
              <label class="lbl">Actions prises</label>
              <input type="text" [(ngModel)]="iForm.actionTaken"
                placeholder="Ex: Appel médecin..." class="finput" />
            </div>

          </div>
        }

        <!-- ═════════ MODAL PDF ═════════ -->
        @if (activeModal() === 'day') {
          <div class="space-y-5">
            <div class="p-4 rounded-xl bg-gradient-to-r from-primary/10 to-primary/5 border border-primary/20">
              <h4 class="font-bold text-base text-primary">Journal du {{ formatDateFr(selectedDate()) }}</h4>
              <p class="text-xs text-muted-foreground mt-1">Exporté depuis Tfakkarni — Suivi Quotidien</p>
            </div>
            <!-- Stats -->
            <div class="grid grid-cols-4 gap-2">
              <div class="text-center p-3 rounded-xl bg-orange-50 border border-orange-100">
                <p class="text-xl font-bold text-orange-600">{{ log()?.nutritionEntries?.length ?? 0 }}</p>
                <p class="text-[10px] text-orange-500 font-medium mt-0.5">Repas</p>
              </div>
              <div class="text-center p-3 rounded-xl bg-blue-50 border border-blue-100">
                <p class="text-xl font-bold text-blue-600">{{ prisTaken() }}/{{ log()?.medicationIntakes?.length ?? 0 }}</p>
                <p class="text-[10px] text-blue-500 font-medium mt-0.5">Méds</p>
              </div>
              <div class="text-center p-3 rounded-xl bg-green-50 border border-green-100">
                <p class="text-xl font-bold text-green-600">{{ totalActMin() }}</p>
                <p class="text-[10px] text-green-500 font-medium mt-0.5">Min activ.</p>
              </div>
              <div class="text-center p-3 rounded-xl bg-red-50 border border-red-100">
                <p class="text-xl font-bold" [class]="(log()?.incidentEntries?.length??0)>0?'text-red-600':'text-slate-400'">{{ log()?.incidentEntries?.length ?? 0 }}</p>
                <p class="text-[10px] text-red-500 font-medium mt-0.5">Incidents</p>
              </div>
            </div>
            <p class="text-xs text-center text-muted-foreground">
              Cliquez sur <strong>Télécharger PDF</strong> pour obtenir le rapport complet au format .pdf
            </p>
          </div>
        }

      </div><!-- /body -->

      <!-- Footer -->
      <div class="flex gap-2 px-6 py-4 border-t border-border bg-background/80 shrink-0">
        @if (activeModal() === 'day') {
          <button z-button class="flex-1"
            [disabled]="pdfLoading()"
            (click)="downloadPdf()">
            @if (pdfLoading()) { <z-icon zType="loader-2" class="mr-2 animate-spin h-4 w-4" /> Génération... }
            @else { <z-icon zType="file" class="h-4 w-4 mr-1.5" /> Télécharger PDF }
          </button>
          <button z-button zType="outline" (click)="closeModal()">Fermer</button>
        } @else {
          <button z-button class="flex-1"
            [disabled]="saving()"
            (click)="submitActiveForm()">
            @if (saving()) { <z-icon zType="loader-2" class="mr-2 animate-spin h-4 w-4" /> }
            Enregistrer
          </button>
          <button z-button zType="outline" class="flex-1" (click)="closeModal()">Annuler</button>
        }
      </div>
    </div><!-- /dialog -->
  </div>
}

<!-- ══ ERROR TOAST ══ -->
@if (errorMsg()) {
  <div class="fixed bottom-4 right-4 max-w-sm bg-destructive text-destructive-foreground
              px-4 py-3 rounded-xl shadow-xl flex items-center gap-2 z-[60]">
    <z-icon zType="circle-x" class="h-4 w-4 shrink-0" />
    <span class="text-sm flex-1">{{ errorMsg() }}</span>
    <button (click)="errorMsg.set('')"><z-icon zType="x" class="h-4 w-4" /></button>
  </div>
}
  `,
  styles: [`
    .lbl        { @apply text-xs font-medium text-muted-foreground mb-1 block; }
    .req        { @apply text-red-500; }
    .ferr       { @apply text-red-500 text-xs mt-1; }
    .ferr::before { content: '⚠ '; }
    .finput     { @apply w-full px-3 py-2 border border-border rounded-lg bg-background text-sm
                         focus:outline-none focus:ring-2 focus:ring-primary transition-colors; }
    .finput-err { @apply border-red-400 focus:ring-red-400; }
    .chip       { @apply px-3 py-1.5 text-xs rounded-lg border font-medium transition-all cursor-pointer; }
    .chip-off   { @apply border-border bg-background; border: 1px solid hsl(var(--border)); transition: all 0.2s; }
    .chip-off:hover { border-color: hsl(var(--primary) / 0.5); }
    .chip-on    { @apply bg-primary text-primary-foreground border-primary shadow-sm; }
  `],
})
export class SuiviQuotidienComponent implements OnInit {
  @Input() keycloakId = '';
  /** Mode lecture seule (vue médecin) — masque les boutons d'ajout/édition/suppression */
  @Input() readOnly = false;
  @Output() goBack = new EventEmitter<void>();

  readonly today    = todayIso();
  readonly pageSize = PAGE_SIZE;

  // ── Core ───────────────────────────────────────────────────────────────
  selectedDate  = signal(todayIso());
  weekStart     = signal(getMondayOf(todayIso()));
  log           = signal<DailyLogResponse | null>(null);
  loading       = signal(false);
  saving        = signal(false);
  pdfLoading    = signal(false);
  errorMsg      = signal('');
  logCache      = signal<Record<string, DailyLogResponse>>({});
  availableMeds = signal<AvailableMedication[]>([]);

  // ── Modal ──────────────────────────────────────────────────────────────
  activeModal   = signal<string | null>(null);
  formSubmitted = signal(false); // devient true au clic "Enregistrer" → affiche les erreurs

  // ── Edit IDs ───────────────────────────────────────────────────────────
  editNId = signal<number | null>(null);
  editMId = signal<number | null>(null);
  editAId = signal<number | null>(null);
  editIId = signal<number | null>(null);

  // ── Form models ────────────────────────────────────────────────────────
  nForm: NutritionEntryRequest      = this.emptyN();
  mForm: MedicationIntakeLogRequest = this.emptyM();
  aForm: ActivityEntryRequest       = this.emptyA();
  iForm: IncidentEntryRequest       = this.emptyI();

  // ── Zod Validation Errors ──────────────────────────────────────────────
  nErrors = signal<Record<string, string>>({});
  mErrors = signal<Record<string, string>>({});
  aErrors = signal<Record<string, string>>({});
  iErrors = signal<Record<string, string>>({});

  // ── Number fields as strings (pour détecter les lettres) ──────────────
  /** Hydratation (Nutrition) – chaîne brute saisie par l'utilisateur */
  nHydroRaw = signal('');
  /** Durée (Activité) – chaîne brute saisie par l'utilisateur */
  aDurRaw   = signal('');

  // ── Erreurs champs numériques ──────────────────────────────────────────
  nHydroErr = computed(() => {
    const v = this.nHydroRaw().trim();
    if (!v) return 'Ce champ est obligatoire';
    if (!/^\d+$/.test(v))         return 'Ce champ doit contenir uniquement des chiffres';
    if (parseInt(v, 10) <= 0)     return 'La valeur doit être supérieure à 0';
    if (parseInt(v, 10) > 10000)  return 'Valeur trop élevée (max 10 000 ml)';
    return '';
  });

  aDurErr = computed(() => {
    const v = this.aDurRaw().trim();
    if (!v) return 'Ce champ est obligatoire';
    if (!/^\d+$/.test(v))        return 'Ce champ doit contenir uniquement des chiffres';
    if (parseInt(v, 10) <= 0)    return 'La durée doit être supérieure à 0';
    if (parseInt(v, 10) > 1440)  return 'La durée ne peut pas dépasser 1440 minutes';
    return '';
  });

  // ── Search / Sort / Pagination ─────────────────────────────────────────
  nSearch = signal(''); nSort = signal(''); nPage = signal(1);
  mSearch = signal(''); mSort = signal(''); mPage = signal(1);
  aSearch = signal(''); aSort = signal(''); aPage = signal(1);
  iSearch = signal(''); iSort = signal(''); iPage = signal(1);

  // ── Week ───────────────────────────────────────────────────────────────
  weekDays      = computed(() => buildWeek(this.weekStart(), this.today));
  isCurrentWeek = computed(() => this.weekStart() === getMondayOf(this.today));
  weekMonthYear = computed(() => {
    const d = new Date(this.weekStart() + 'T12:00:00');
    return `${String(d.getMonth()+1).padStart(2,'0')}/${d.getFullYear()}`;
  });

  // ── Stats ──────────────────────────────────────────────────────────────
  prisTaken  = computed(() => this.log()?.medicationIntakes?.filter(m => m.status==='PRIS').length ?? 0);
  totalActMin = computed(() => this.log()?.activityEntries?.reduce((s,a)=>s+(a.durationMinutes??0),0) ?? 0);

  // ── Filtered + Sorted ──────────────────────────────────────────────────
  nFiltered = computed(() => {
    const q = this.nSearch().toLowerCase().trim(); const s = this.nSort();
    let l = [...(this.log()?.nutritionEntries ?? [])];
    if (q) l = l.filter(e => e.description.toLowerCase().includes(q) || MEAL_LABELS[e.mealType].toLowerCase().includes(q));
    if (s==='time')     l.sort((a,b)=>(a.entryTime??'').localeCompare(b.entryTime??''));
    if (s==='type')     l.sort((a,b)=>a.mealType.localeCompare(b.mealType));
    if (s==='quantity') { const o:Record<string,number>={RIEN:0,PEU:1,DEMI:2,COMPLET:3}; l.sort((a,b)=>(o[b.quantity]??0)-(o[a.quantity]??0)); }
    return l;
  });
  mFiltered = computed(() => {
    const q = this.mSearch().toLowerCase().trim(); const s = this.mSort();
    let l = [...(this.log()?.medicationIntakes ?? [])];
    if (q) l = l.filter(e => e.medicationName.toLowerCase().includes(q) || e.status.toLowerCase().includes(q));
    if (s==='name')   l.sort((a,b)=>a.medicationName.localeCompare(b.medicationName));
    if (s==='status') l.sort((a,b)=>a.status.localeCompare(b.status));
    if (s==='time')   l.sort((a,b)=>(a.takenAt??'').localeCompare(b.takenAt??''));
    return l;
  });
  aFiltered = computed(() => {
    const q = this.aSearch().toLowerCase().trim(); const s = this.aSort();
    let l = [...(this.log()?.activityEntries ?? [])];
    if (q) l = l.filter(e => e.description.toLowerCase().includes(q) || e.activityType.toLowerCase().includes(q));
    if (s==='type')     l.sort((a,b)=>a.activityType.localeCompare(b.activityType));
    if (s==='duration') l.sort((a,b)=>(b.durationMinutes??0)-(a.durationMinutes??0));
    if (s==='intensity'){ const o:Record<string,number>={ELEVE:3,MODERE:2,FAIBLE:1}; l.sort((a,b)=>(o[b.intensity??'']??0)-(o[a.intensity??'']??0)); }
    if (s==='time')     l.sort((a,b)=>(a.startTime??'').localeCompare(b.startTime??''));
    return l;
  });
  iFiltered = computed(() => {
    const q = this.iSearch().toLowerCase().trim(); const s = this.iSort();
    let l = [...(this.log()?.incidentEntries ?? [])];
    if (q) l = l.filter(e => e.description.toLowerCase().includes(q) || e.incidentType.toLowerCase().includes(q) || (e.location?.toLowerCase().includes(q)));
    if (s==='severity'){ const r:Record<string,number>={GRAVE:3,MODERE:2,LEGER:1}; l.sort((a,b)=>(r[b.severity]??0)-(r[a.severity]??0)); }
    if (s==='type')    l.sort((a,b)=>a.incidentType.localeCompare(b.incidentType));
    if (s==='time')    l.sort((a,b)=>(a.occurredAt??'').localeCompare(b.occurredAt??''));
    return l;
  });

  // Reset pages on filter change
  private _rN = effect(()=>{ this.nFiltered(); this.nPage.set(1); },{ allowSignalWrites:true });
  private _rM = effect(()=>{ this.mFiltered(); this.mPage.set(1); },{ allowSignalWrites:true });
  private _rA = effect(()=>{ this.aFiltered(); this.aPage.set(1); },{ allowSignalWrites:true });
  private _rI = effect(()=>{ this.iFiltered(); this.iPage.set(1); },{ allowSignalWrites:true });

  nPageItems = computed(()=>this.nFiltered().slice((this.nPage()-1)*PAGE_SIZE, this.nPage()*PAGE_SIZE));
  mPageItems = computed(()=>this.mFiltered().slice((this.mPage()-1)*PAGE_SIZE, this.mPage()*PAGE_SIZE));
  aPageItems = computed(()=>this.aFiltered().slice((this.aPage()-1)*PAGE_SIZE, this.aPage()*PAGE_SIZE));
  iPageItems = computed(()=>this.iFiltered().slice((this.iPage()-1)*PAGE_SIZE, this.iPage()*PAGE_SIZE));

  totPages(n:number){ return Math.ceil(n/PAGE_SIZE); }
  min(a:number,b:number){ return Math.min(a,b); }
  changePage(tab:string,p:number){
    if(tab==='n') this.nPage.set(p);
    if(tab==='m') this.mPage.set(p);
    if(tab==='a') this.aPage.set(p);
    if(tab==='i') this.iPage.set(p);
  }

  // ── Static option lists ────────────────────────────────────────────────
  intakeStatusList = Object.entries(STATUS_CFG).map(([v,c])=>({ value:v as IntakeStatus, ...c }));
  actTypeList  = Object.entries(ACT_ICONS).map(([v,icon])=>({ value:v as ActivityType, icon, label:v.charAt(0)+v.slice(1).toLowerCase() }));
  incTypeList  = Object.entries(INC_ICONS).map(([v,icon])=>({ value:v as IncidentType, icon, label:v.charAt(0)+v.slice(1).toLowerCase() }));
  sevList = [
    { value:'LEGER'  as SeverityLevel, label:'🟡 Léger',  activeClass:'bg-amber-500 text-white border-amber-500' },
    { value:'MODERE' as SeverityLevel, label:'🟠 Modéré', activeClass:'bg-orange-500 text-white border-orange-500' },
    { value:'GRAVE'  as SeverityLevel, label:'🔴 Grave',  activeClass:'bg-red-600 text-white border-red-600' },
  ];

  // ── Modal helpers ──────────────────────────────────────────────────────
  modalTitle = computed(()=>{
    const m = this.activeModal();
    if (m==='nutrition')  return 'Formulaire alimentation';
    if (m==='medication') return 'Formulaire médicament';
    if (m==='activity')   return 'Formulaire activité';
    if (m==='incident')   return 'Formulaire incident';
    if (m==='day')        return 'Rapport du jour';
    return '';
  });
  modalIcon = computed(():ZardIcon=>{
    const m = this.activeModal();
    if (m==='nutrition')  return 'star';
    if (m==='medication') return 'pill';
    if (m==='activity')   return 'activity';
    if (m==='incident')   return 'triangle-alert';
    return 'file-text';
  });

  closeModal(){
    this.activeModal.set(null);
    this.formSubmitted.set(false);
  }
  openDayModal(){ this.activeModal.set('day'); }

  openNutritionForm(e?: NutritionEntryResponse){
    this.formSubmitted.set(false);
    if(e){ this.editNId.set(e.id); this.nForm={mealType:e.mealType,description:e.description,quantity:e.quantity,appetite:e.appetite,hydrationMl:e.hydrationMl,notes:e.notes,entryTime:e.entryTime};
           this.nHydroRaw.set(e.hydrationMl!=null ? String(e.hydrationMl) : ''); }
    else { this.editNId.set(null); this.nForm=this.emptyN(); this.nHydroRaw.set(''); }
    this.activeModal.set('nutrition');
  }
  openMedForm(e?: MedicationIntakeLogResponse){
    this.formSubmitted.set(false);
    if(e){ this.editMId.set(e.id); this.mForm={medicationId:e.medicationId,takenAt:e.takenAt,status:e.status,notes:e.notes}; }
    else { this.editMId.set(null); this.mForm=this.emptyM(); }
    this.activeModal.set('medication');
  }
  openActivityForm(e?: ActivityEntryResponse){
    this.formSubmitted.set(false);
    if(e){ this.editAId.set(e.id); this.aForm={activityType:e.activityType,description:e.description,durationMinutes:e.durationMinutes,intensity:e.intensity,notes:e.notes,startTime:e.startTime};
           this.aDurRaw.set(e.durationMinutes!=null ? String(e.durationMinutes) : ''); }
    else { this.editAId.set(null); this.aForm=this.emptyA(); this.aDurRaw.set(''); }
    this.activeModal.set('activity');
  }
  openIncidentForm(e?: IncidentEntryResponse){
    this.formSubmitted.set(false);
    if(e){ this.editIId.set(e.id); this.iForm={incidentType:e.incidentType,description:e.description,severity:e.severity,location:e.location,actionTaken:e.actionTaken,injuryDetails:e.injuryDetails,occurredAt:e.occurredAt}; }
    else { this.editIId.set(null); this.iForm=this.emptyI(); }
    this.activeModal.set('incident');
  }

  // ── Validation avant enregistrement ───────────────────────────────────

  /** Retourne true si le formulaire actif est entièrement valide */
  private formIsValid(): boolean {
    const m = this.activeModal();
    if (m === 'nutrition') {
      return (
        !!this.nForm.mealType &&
        !!this.nForm.entryTime &&
        !!this.nForm.description?.trim() &&
        !this.nHydroErr()  // champ hydratation valide
      );
    }
    if (m === 'medication') {
      return (
        !!this.mForm.medicationId &&
        !!this.mForm.takenAt &&
        !!this.mForm.status
      );
    }
    if (m === 'activity') {
      return (
        !!this.aForm.activityType &&
        !!this.aForm.description?.trim() &&
        !!this.aForm.startTime &&
        !this.aDurErr()  // champ durée valide
      );
    }
    if (m === 'incident') {
      return (
        !!this.iForm.incidentType &&
        !!this.iForm.description?.trim() &&
        !!this.iForm.occurredAt &&
        !!this.iForm.location?.trim()
      );
    }
    return false;
  }

  submitActiveForm(){
    // 1. Marquer le formulaire comme soumis → affiche tous les messages d'erreur
    this.formSubmitted.set(true);

    // 2. Vérifier la validité → bloquer si invalide
    if (!this.formIsValid()) return;

    const m = this.activeModal();
    if (m === 'nutrition')  this.saveNutrition();
    if (m === 'medication') this.saveMedication();
    if (m === 'activity')   this.saveActivity();
    if (m === 'incident')   this.saveIncident();
  }

  // ── PDF Download (via jsPDF) ───────────────────────────────────────────

  async downloadPdf(): Promise<void> {
    this.pdfLoading.set(true);
    try {
      // Import dynamique — nécessite : npm install jspdf
      const { jsPDF } = await import('jspdf');
      const doc = new jsPDF({ orientation:'portrait', unit:'mm', format:'a4' });
      const log = this.log();
      const date = this.formatDateFr(this.selectedDate());
      const pageW = doc.internal.pageSize.getWidth();
      const pageH = doc.internal.pageSize.getHeight();
      const margin = 15;
      const colW = pageW - margin * 2;
      let y = 20;

      const nl = (extra=0)=>{ y += 6+extra; if(y>270){ doc.addPage(); y=20; } };
      const line = ()=>{ doc.setDrawColor(220,220,220); doc.line(margin,y,pageW-margin,y); nl(2); };

      // ── En-tête ──
      doc.setFillColor(109,40,217);
      doc.rect(0,0,pageW,28,'F');
      doc.setTextColor(255,255,255);
      doc.setFontSize(16); doc.setFont('helvetica','bold');
      doc.text('TFAKKARNI - Journal Quotidien', margin, 12);
      doc.setFontSize(9); doc.setFont('helvetica','normal');
      doc.text('Suivi du ' + date, margin, 20);
      const nowD = new Date();
      const nowStr = nowD.toLocaleDateString('fr-FR',{day:'2-digit',month:'long',year:'numeric'})
        + ' - ' + nowD.toLocaleTimeString('fr-FR',{hour:'2-digit',minute:'2-digit'});
      doc.text('Genere le ' + nowStr, pageW-margin, 20, { align:'right' });
      y = 36;

      // ── Stats ──
      const boxes = [
        { label:'Repas',        val:String(log?.nutritionEntries?.length??0),                          r:234, g:88,  b:12  },
        { label:'Medicaments',  val:(this.prisTaken())+'/'+(log?.medicationIntakes?.length??0)+' pris', r:29,  g:78,  b:216 },
        { label:'Activite',     val:(this.totalActMin())+' min',                                       r:21,  g:128, b:61  },
        { label:'Incidents',    val:String(log?.incidentEntries?.length??0),                           r:185, g:28,  b:28  },
      ];
      const bw = (colW - 9) / 4;
      boxes.forEach((b,i)=>{
        const x = margin + i*(bw+3);
        const fr=Math.min(255,b.r+Math.round((255-b.r)*0.87));
        const fg=Math.min(255,b.g+Math.round((255-b.g)*0.87));
        const fb=Math.min(255,b.b+Math.round((255-b.b)*0.87));
        doc.setFillColor(fr,fg,fb);
        doc.roundedRect(x,y,bw,18,3,3,'F');
        doc.setFillColor(b.r,b.g,b.b);
        doc.rect(x,y,bw,2.5,'F');
        doc.setTextColor(b.r,b.g,b.b);
        doc.setFontSize(13); doc.setFont('helvetica','bold');
        doc.text(b.val, x+bw/2, y+9, { align:'center' });
        doc.setFontSize(7); doc.setFont('helvetica','normal');
        doc.text(b.label, x+bw/2, y+14.5, { align:'center' });
      });
      y += 24;

      // ── Helpers section ──
      const section = (title:string, r:number, g:number, b:number)=>{
        nl(2);
        const pr=Math.min(255,r+Math.round((255-r)*0.92));
        const pg=Math.min(255,g+Math.round((255-g)*0.92));
        const pb=Math.min(255,b+Math.round((255-b)*0.92));
        doc.setFillColor(r,g,b); doc.rect(margin, y-1, 4, 8, 'F');
        doc.setFillColor(pr,pg,pb); doc.rect(margin+4, y-1, colW-4, 8, 'F');
        doc.setTextColor(r,g,b); doc.setFontSize(10); doc.setFont('helvetica','bold');
        doc.text(title, margin+7, y+4.5);
        doc.setTextColor(30,30,30); y+=11;
      };

      const row = (label:string, value:string)=>{
        if(!value) return;
        doc.setFontSize(8); doc.setFont('helvetica','bold'); doc.setTextColor(55,55,65);
        doc.text(label, margin+4, y);
        const lw = doc.getTextWidth(label)+2;
        doc.setFont('helvetica','normal'); doc.setTextColor(75,75,85);
        const lines = doc.splitTextToSize(value, colW - lw - 4);
        doc.text(lines, margin+4+lw, y);
        y += 4+(lines.length-1)*3.8;
        if(y>272){doc.addPage();y=20;}
      };

      const emptyNote = (msg:string)=>{
        doc.setFontSize(8); doc.setFont('helvetica','italic'); doc.setTextColor(150,150,150);
        doc.text(msg, margin+4, y); nl(); doc.setTextColor(30,30,30);
      };

      const itemSep = ()=>{
        doc.setDrawColor(235,235,240); doc.setLineWidth(0.25);
        doc.line(margin+4, y, pageW-margin, y); y+=3;
      };

      // ── Alimentation ──
      section('ALIMENTATION', 234, 88, 12);
      if ((log?.nutritionEntries?.length??0) > 0) {
        log!.nutritionEntries.forEach((e,i)=>{
          if(i>0) itemSep();
          row('Type :',       MEAL_LABELS[e.mealType]);
          row('Heure :',      e.entryTime ?? 'Non renseigne');
          row('Aliments :',   e.description);
          row('Quantite :',   e.quantity);
          row('Appetit :',    e.appetite ?? '');
          if(e.hydrationMl) row('Hydratation :', e.hydrationMl+' ml');
          if(e.notes)       row('Notes :', e.notes);
          nl(1);
        });
      } else { emptyNote('Aucun repas enregistre.'); }
      line();

      // ── Medicaments ──
      section('MEDICAMENTS', 29, 78, 216);
      if ((log?.medicationIntakes?.length??0) > 0) {
        log!.medicationIntakes.forEach((e,i)=>{
          if(i>0) itemSep();
          row('Medicament :', e.medicationName + (e.dosage ? '  '+e.dosage : ''));
          row('Statut :',    STATUS_CFG[e.status]?.label ?? e.status);
          if(e.takenAt)   row('Pris a :', e.takenAt);
          if(e.frequency) row('Frequence :', e.frequency);
          if(e.notes)     row('Notes :', e.notes);
          nl(1);
        });
      } else { emptyNote('Aucune prise medicamenteuse.'); }
      line();

      // ── Activites ──
      section('ACTIVITES', 21, 128, 61);
      if ((log?.activityEntries?.length??0) > 0) {
        log!.activityEntries.forEach((e,i)=>{
          if(i>0) itemSep();
          row('Description :', e.description);
          row('Type :',        e.activityType);
          if(e.durationMinutes) row('Duree :', e.durationMinutes+' minutes');
          if(e.startTime)       row('Heure debut :', e.startTime);
          if(e.intensity)       row('Intensite :', e.intensity);
          if(e.notes)           row('Notes :', e.notes);
          nl(1);
        });
      } else { emptyNote('Aucune activite enregistree.'); }
      line();

      // ── Incidents ──
      section('INCIDENTS', 185, 28, 28);
      if ((log?.incidentEntries?.length??0) > 0) {
        log!.incidentEntries.forEach((e,i)=>{
          if(i>0) itemSep();
          row('Type :',        e.incidentType);
          row('Gravite :',     SEVERITY_CFG[e.severity]?.label ?? e.severity);
          if(e.occurredAt)    row('Heure :', e.occurredAt);
          row('Description :', e.description);
          if(e.location)      row('Lieu :', e.location);
          if(e.injuryDetails) row('Blessures :', e.injuryDetails);
          if(e.actionTaken)   row('Action prise :', e.actionTaken);
          nl(1);
        });
      } else {
        doc.setFontSize(8); doc.setFont('helvetica','bold'); doc.setTextColor(21,128,61);
        doc.text('Aucun incident signale - journee calme !', margin+4, y);
        nl(); doc.setTextColor(30,30,30);
      }

      // ── Pied de page ──
      const totalPages = (doc as any).internal.getNumberOfPages();
      for(let p=1;p<=totalPages;p++){
        doc.setPage(p);
        doc.setFillColor(248,248,252); doc.rect(0,pageH-12,pageW,12,'F');
        doc.setDrawColor(220,220,230); doc.setLineWidth(0.3); doc.line(0,pageH-12,pageW,pageH-12);
        doc.setFontSize(7); doc.setTextColor(160,160,170); doc.setFont('helvetica','normal');
        doc.text('Tfakkarni - Module Suivi Quotidien - Document confidentiel', margin, pageH-4.5);
        doc.text('Page '+p+' / '+totalPages, pageW-margin, pageH-4.5, { align:'right' });
      }

      doc.save('journal-'+this.selectedDate()+'.pdf');
    } catch (err) {
      console.error('jsPDF error:', err);
      this.errorMsg.set('Erreur PDF. Verifiez que jsPDF est installe : npm install jspdf');
    }
    this.pdfLoading.set(false);
  }

  // ── Data loading ───────────────────────────────────────────────────────
  constructor(private readonly svc: DailyMonitoringService) {}

  ngOnInit(){ this.loadLog(); this.loadAvailableMeds(); }

  loadLog(){
    this.loading.set(true);
    this.svc.getOrCreateLogForDate(this.keycloakId, this.selectedDate()).subscribe({
      next: log=>{ this.log.set(log); this.logCache.update(c=>({...c,[log.logDate]:log})); this.loading.set(false); },
      error: ()=>{ this.log.set(null); this.loading.set(false); },
    });
  }

  loadAvailableMeds(){
    this.svc.getAvailableMedications(this.keycloakId).subscribe({
      next: m=>this.availableMeds.set(m), error:()=>{},
    });
  }

  // ── Week navigation ────────────────────────────────────────────────────
  prevWeek(){ this.weekStart.set(addDaysIso(this.weekStart(),-7)); }
  nextWeek(){ if(!this.isCurrentWeek()) this.weekStart.set(addDaysIso(this.weekStart(),7)); }
  selectDay(iso:string){ if(iso>this.today) return; this.selectedDate.set(iso); this.loadLog(); }

  dayBtnClass(day:WeekDay):string{
    const b='flex flex-col items-center py-2 px-1 rounded-xl transition-all text-sm font-medium ';
    if(day.iso===this.selectedDate()) return b+'bg-primary text-primary-foreground shadow-md';
    if(day.isToday)  return b+'bg-primary/10 text-primary border border-primary/30';
    if(day.isFuture) return b+'opacity-25 cursor-not-allowed text-muted-foreground';
    return b+'hover:bg-muted cursor-pointer';
  }

  totalEntries(log:DailyLogResponse):string{
    const n=(log.nutritionEntries?.length??0)+(log.medicationIntakes?.length??0)+(log.activityEntries?.length??0)+(log.incidentEntries?.length??0);
    return n>0?n+' entr.':'';
  }

  // ── Utility ────────────────────────────────────────────────────────────
  getVal(e:Event){ return (e.target as HTMLInputElement|HTMLSelectElement).value; }

  formatDateFr(iso:string):string{
    const d=new Date(iso+'T12:00:00');
    const days=['Dimanche','Lundi','Mardi','Mercredi','Jeudi','Vendredi','Samedi'];
    const months=['janvier','février','mars','avril','mai','juin','juillet','août','septembre','octobre','novembre','décembre'];
    return `${days[d.getDay()]} ${d.getDate()} ${months[d.getMonth()]} ${d.getFullYear()}`;
  }

  // ── Display helpers ────────────────────────────────────────────────────
  mealLabel(t:MealType):string   { return MEAL_LABELS[t]??t; }
  mealIcon(t:MealType):ZardIcon  { return MEAL_ICONS[t]??'star'; }
  statusCfg(s:IntakeStatus)      { return STATUS_CFG[s]??STATUS_CFG['OUBLIE']; }
  sevCfg(s:SeverityLevel)        { return SEVERITY_CFG[s]??SEVERITY_CFG['LEGER']; }
  actIcon(t:ActivityType):ZardIcon{ return ACT_ICONS[t]??'star'; }
  incIcon(t:IncidentType):ZardIcon{ return INC_ICONS[t]??'shield'; }
  incBorder(s:SeverityLevel){ return s==='GRAVE'?'border-l-red-600':s==='MODERE'?'border-l-orange-500':'border-l-amber-400'; }
  qtyBadge(q:QuantityLevel){ return q==='COMPLET'?'bg-emerald-100 text-emerald-700 border border-emerald-200':q==='DEMI'?'bg-yellow-100 text-yellow-700 border border-yellow-200':q==='PEU'?'bg-orange-100 text-orange-700 border border-orange-200':'bg-red-100 text-red-700 border border-red-200'; }

  // ── Edit shortcuts ─────────────────────────────────────────────────────
  editNutrition(e:NutritionEntryResponse)       { this.openNutritionForm(e); }
  editMedication(e:MedicationIntakeLogResponse) { this.openMedForm(e); }
  editActivity(e:ActivityEntryResponse)         { this.openActivityForm(e); }
  editIncident(e:IncidentEntryResponse)         { this.openIncidentForm(e); }

  // ── Save methods ───────────────────────────────────────────────────────
  saveNutrition(){
    const logId=this.log()?.id; if(!logId) return;
    // Injecter la valeur hydratation parsée
    this.nForm.hydrationMl = this.nHydroRaw() ? parseInt(this.nHydroRaw(), 10) : undefined;
    
    // Valider avec Zod
    const result = NutritionSchema.safeParse(this.nForm);
    if (!result.success) {
      const errors: Record<string, string> = {};
      result.error.issues.forEach((e: any) => {
        const path = e.path.join('.');
        errors[path] = e.message;
      });
      this.nErrors.set(errors);
      return;
    }
    
    this.nErrors.set({}); // Clear errors
    this.saving.set(true);
    const id=this.editNId();
    (id?this.svc.updateNutrition(logId,id,this.nForm):this.svc.addNutrition(logId,this.nForm))
      .subscribe({ next:()=>{ this.saving.set(false); this.closeModal(); this.loadLog(); },
                   error:()=>{ this.saving.set(false); this.errorMsg.set('Erreur lors de l\'enregistrement.'); } });
  }

  deleteNutrition(id:number){
    const logId=this.log()?.id; if(!logId) return;
    this.svc.deleteNutrition(logId,id).subscribe({ next:()=>this.loadLog(), error:()=>this.errorMsg.set('Erreur suppression.') });
  }

  saveMedication(){
    const logId=this.log()?.id; if(!logId) return;
    
    // Valider avec Zod
    const result = MedicationSchema.safeParse(this.mForm);
    if (!result.success) {
      const errors: Record<string, string> = {};
      result.error.issues.forEach((e: any) => {
        const path = e.path.join('.');
        errors[path] = e.message;
      });
      this.mErrors.set(errors);
      return;
    }
    
    this.mErrors.set({}); // Clear errors
    this.saving.set(true);
    const id=this.editMId();
    (id?this.svc.updateMedicationIntake(logId,id,this.mForm):this.svc.addMedicationIntake(logId,this.mForm))
      .subscribe({ next:()=>{ this.saving.set(false); this.closeModal(); this.loadLog(); },
                   error:()=>{ this.saving.set(false); this.errorMsg.set('Erreur lors de l\'enregistrement.'); } });
  }

  deleteMedication(id:number){
    const logId=this.log()?.id; if(!logId) return;
    this.svc.deleteMedicationIntake(logId,id).subscribe({ next:()=>this.loadLog(), error:()=>this.errorMsg.set('Erreur suppression.') });
  }

  saveActivity(){
    const logId=this.log()?.id; if(!logId) return;
    // Injecter la valeur durée parsée
    this.aForm.durationMinutes = this.aDurRaw() ? parseInt(this.aDurRaw(), 10) : undefined;
    
    // Valider avec Zod
    const result = ActivitySchema.safeParse(this.aForm);
    if (!result.success) {
      const errors: Record<string, string> = {};
      result.error.issues.forEach((e: any) => {
        const path = e.path.join('.');
        errors[path] = e.message;
      });
      this.aErrors.set(errors);
      return;
    }
    
    this.aErrors.set({}); // Clear errors
    this.saving.set(true);
    const id=this.editAId();
    (id?this.svc.updateActivity(logId,id,this.aForm):this.svc.addActivity(logId,this.aForm))
      .subscribe({ next:()=>{ this.saving.set(false); this.closeModal(); this.loadLog(); },
                   error:()=>{ this.saving.set(false); this.errorMsg.set('Erreur lors de l\'enregistrement.'); } });
  }

  deleteActivity(id:number){
    const logId=this.log()?.id; if(!logId) return;
    this.svc.deleteActivity(logId,id).subscribe({ next:()=>this.loadLog(), error:()=>this.errorMsg.set('Erreur suppression.') });
  }

  saveIncident(){
    const logId=this.log()?.id; if(!logId) return;
    
    // Valider avec Zod
    const result = IncidentSchema.safeParse(this.iForm);
    if (!result.success) {
      const errors: Record<string, string> = {};
      result.error.issues.forEach((e: any) => {
        const path = e.path.join('.');
        errors[path] = e.message;
      });
      this.iErrors.set(errors);
      return;
    }
    
    this.iErrors.set({}); // Clear errors
    this.saving.set(true);
    const id=this.editIId();
    (id?this.svc.updateIncident(logId,id,this.iForm):this.svc.addIncident(logId,this.iForm))
      .subscribe({ next:()=>{ this.saving.set(false); this.closeModal(); this.loadLog(); },
                   error:()=>{ this.saving.set(false); this.errorMsg.set('Erreur lors de l\'enregistrement.'); } });
  }

  deleteIncident(id:number){
    const logId=this.log()?.id; if(!logId) return;
    this.svc.deleteIncident(logId,id).subscribe({ next:()=>this.loadLog(), error:()=>this.errorMsg.set('Erreur suppression.') });
  }

  // ── Empty form factories ───────────────────────────────────────────────
  private emptyN():NutritionEntryRequest      { return { mealType:'BREAKFAST', description:'', quantity:'COMPLET', appetite:'BON' }; }
  private emptyM():MedicationIntakeLogRequest { return { medicationId:undefined as any, takenAt:'', status:'PRIS', notes:'' }; }
  private emptyA():ActivityEntryRequest       { return { activityType:'PHYSIQUE', description:'', intensity:'MODERE' }; }
  private emptyI():IncidentEntryRequest       { return { incidentType:'CHUTE', description:'', severity:'LEGER' }; }
}
