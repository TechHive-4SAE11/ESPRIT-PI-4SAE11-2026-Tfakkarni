import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatbotService, ChatMessage, ChatResponse } from '@/core/services/chatbot.service';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardCardComponent } from '@/shared/components/card';
import { UserApiService } from '@/core/services/user-api.service';
import { KeycloakService } from 'keycloak-angular';

@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [CommonModule, FormsModule, ZardButtonComponent, ZardCardComponent],
  template: `
    <div class="p-6 max-w-4xl mx-auto flex flex-col h-screen">
      <div class="flex items-center mb-6">
        <z-button zType="outline" (click)="goBack()">← Retour</z-button>
        <h1 class="text-2xl font-bold ml-4">Assistant IA</h1>
      </div>

      <z-card class="flex-1 flex flex-col mb-4 overflow-hidden">
        <div class="flex-1 overflow-y-auto p-4 flex flex-col gap-4">
          <div *ngFor="let msg of messages()"
               [ngClass]="{'self-end bg-blue-100 text-blue-900': msg.role === 'USER', 'self-start bg-gray-100 text-gray-900': msg.role === 'AI'}"
               class="max-w-[80%] rounded-lg p-3">
            <p>{{ msg.content }}</p>
          </div>
          <div *ngIf="isLoading()" class="self-start text-gray-500 italic p-3">
            L'IA écrit...
          </div>
        </div>
      </z-card>

      <div class="flex gap-2">
        <input type="text"
               class="flex-1 border rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
               [(ngModel)]="question"
               [disabled]="isLoading()"
               placeholder="Posez votre question...">
        <z-button (click)="sendMessage()" [zDisabled]="isLoading() || !question.trim()">
          Envoyer
        </z-button>
      </div>
    </div>
  `
})
export class ChatbotComponent implements OnInit {
  private chatbotService = inject(ChatbotService);
  private userApiService = inject(UserApiService);
  private keycloakService = inject(KeycloakService);
  private location = inject(Location);

  messages = signal<ChatMessage[]>([]);
  isLoading = signal<boolean>(false);
  question = '';
  sessionId?: number;
  userId: number = 1;

  async ngOnInit() {
    try {
      const profile = await this.keycloakService.loadUserProfile();
      if (profile.id) {
        this.userApiService.getUserByKeycloakId(profile.id).subscribe(user => {
          this.userId = user.id;
        });
      }
    } catch (e) {
      console.warn('Utilisateur non connecté, userId = 1 par défaut.');
    }
  }

  goBack() {
    this.location.back();
  }

  sendMessage() {
    if (!this.question.trim() || this.isLoading()) return;

    const userMsg = this.question;
    this.question = '';

    this.messages.update(m => [...m, { role: 'USER', content: userMsg, timestamp: new Date().toISOString() }]);
    this.isLoading.set(true);

    this.chatbotService.sendMessage(this.userId, userMsg, this.sessionId).subscribe({
      next: (res: ChatResponse) => {
        this.sessionId = res.sessionId;
        this.messages.update(m => [...m, { role: 'AI', content: res.answer, timestamp: new Date().toISOString() }]);
        this.isLoading.set(false);
      },
      error: () => {
        this.messages.update(m => [...m, {
          role: 'AI',
          content: "Je suis désolé, le service IA est temporairement indisponible. En attendant, voici un conseil : prenez 5 minutes pour respirer profondément.",
          timestamp: new Date().toISOString()
        }]);
        this.isLoading.set(false);
      }
    });
  }
}
