import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { DashboardLayoutComponent, type SidebarMenuGroup } from '@/shared/components/dashboard-layout';
import { AiQuizGeneratorComponent } from './ai-quiz-generator/ai-quiz-generator.component';
import { AiEquipmentRecommenderComponent } from './ai-equipment-recommender/ai-equipment-recommender.component';
import { AiVoiceAssistantComponent } from './ai-voice-assistant/ai-voice-assistant.component';
import { AiMemoryVideosComponent } from './ai-memory-videos/ai-memory-videos.component';

@Component({
  selector: 'app-ai-assistant-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    DashboardLayoutComponent,
    AiQuizGeneratorComponent,
    AiEquipmentRecommenderComponent,
    AiVoiceAssistantComponent,
    AiMemoryVideosComponent,
  ],
  template: `
    <app-dashboard-layout
      [menuGroups]="menuGroups"
      [pageTitle]="currentPage()"
      basePath="/ai-assistant"
    >
      <div class="space-y-4">
        @switch (currentPage()) {
          @case ('AI Quiz Generator') {
            <app-ai-quiz-generator />
          }
          @case ('Equipment Recommender') {
            <app-ai-equipment-recommender />
          }
          @case ('Voice Assistant') {
            <app-ai-voice-assistant />
          }
          @case ('Memory Videos') {
            <app-ai-memory-videos />
          }
          @default {
            <!-- Dashboard Home -->
            <div class="space-y-6">
              <div>
                <h2 class="text-3xl font-bold bg-gradient-to-r from-violet-600 via-fuchsia-500 to-rose-500 bg-clip-text text-transparent">
                  🤖 AI Assistant Hub
                </h2>
                <p class="text-sm text-muted-foreground mt-1">Powered by GPT-4o-mini • Orchestrates Game & Medical services</p>
              </div>

              <div class="grid gap-4 md:grid-cols-2">
                @for (card of featureCards; track card.title) {
                  <div (click)="setPage(card.page)"
                    class="group rounded-2xl border border-border bg-card p-6 shadow-sm hover:shadow-lg hover:border-primary/30 transition-all cursor-pointer">
                    <div class="flex items-start gap-4">
                      <div class="p-3 rounded-2xl text-white shrink-0" [class]="card.gradient">
                        <span class="text-2xl">{{ card.emoji }}</span>
                      </div>
                      <div class="flex-1 min-w-0">
                        <h3 class="font-bold text-lg text-foreground group-hover:text-primary transition-colors">{{ card.title }}</h3>
                        <p class="text-sm text-muted-foreground mt-1 leading-relaxed">{{ card.description }}</p>

                      </div>
                    </div>
                  </div>
                }
              </div>


            </div>
          }
        }
      </div>
    </app-dashboard-layout>
  `,
})
export class AiAssistantDashboardComponent {
  private readonly router = inject(Router);
  currentPage = signal('Home');

  menuGroups: SidebarMenuGroup[] = [
    {
      label: 'AI Features',
      items: [
        { icon: 'house', label: 'Home', action: () => this.setPage('Home') },
        { icon: 'brain', label: 'AI Quiz Generator', action: () => this.setPage('AI Quiz Generator') },
        { icon: 'heart', label: 'Equipment Recommender', action: () => this.setPage('Equipment Recommender') },
        { icon: 'sparkles', label: 'Voice Assistant', action: () => this.setPage('Voice Assistant') },
        { icon: 'circle-play', label: 'Memory Videos', action: () => this.setPage('Memory Videos') },
      ],
    },
    {
      label: 'Navigation',
      items: [
        { icon: 'arrow-left', label: 'Back to Dashboard', action: () => this.router.navigate(['/patient']) },
      ],
    },
  ];

  featureCards = [
    {
      title: 'AI Quiz Generator',
      description: 'Generate cognitive assessment quizzes with custom topics, difficulty levels, and number of questions. Quizzes are automatically saved to game-service.',
      emoji: '🧠',
      gradient: 'bg-gradient-to-br from-violet-500 to-fuchsia-500',

      page: 'AI Quiz Generator',
    },
    {
      title: 'Equipment Recommender',
      description: 'Get AI-powered top-3 equipment recommendations based on patient condition and severity. Pulls real inventory from medical-service.',
      emoji: '🏥',
      gradient: 'bg-gradient-to-br from-cyan-500 to-blue-600',

      page: 'Equipment Recommender',
    },
    {
      title: 'Voice Assistant',
      description: 'Chat-based interface for natural language commands. Borrow/return equipment, generate quizzes, check status.',
      emoji: '🎙️',
      gradient: 'bg-gradient-to-br from-indigo-500 to-purple-600',

      page: 'Voice Assistant',
    },
    {
      title: 'Memory Videos',
      description: 'Create personalized video scripts and storyboards for memory stimulation. Supports Photo, Story, and Exercise types.',
      emoji: '🎬',
      gradient: 'bg-gradient-to-br from-rose-500 to-orange-500',

      page: 'Memory Videos',
    },
  ];

  setPage(page: string): void {
    this.currentPage.set(page);
  }
}
