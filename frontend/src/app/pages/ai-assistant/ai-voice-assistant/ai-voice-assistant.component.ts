import { Component, signal, ElementRef, ViewChild, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AssistantAIService } from '@/core/services/assistant-ai.service';
import { VoiceCommandResponse } from '@/core/models/assistant-ai.model';

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  type?: string;
  data?: any;
  timestamp: Date;
}

@Component({
  selector: 'app-ai-voice-assistant',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6">
      <!-- Header -->
      <div class="flex items-center gap-3">
        <div class="p-3 rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 text-white">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z"/>
          </svg>
        </div>
        <div>
          <h2 class="text-2xl font-bold bg-gradient-to-r from-indigo-600 to-purple-600 bg-clip-text text-transparent">
            AI Voice Assistant
          </h2>
          <p class="text-sm text-muted-foreground">Interact with Tfakkarni via natural language commands</p>
        </div>
      </div>

      <div class="grid gap-6 lg:grid-cols-4">
        <!-- Quick Commands -->
        <div class="lg:col-span-1">
          <div class="rounded-2xl border border-border bg-card p-5 shadow-sm space-y-4">
            <h3 class="font-semibold text-sm flex items-center gap-2">
              <span>⚡</span> Quick Commands
            </h3>
            @for (cmd of quickCommands; track cmd.command) {
              <button
                (click)="sendQuickCommand(cmd.command)"
                [disabled]="isSending()"
                class="w-full text-left p-3 rounded-xl border border-input bg-background hover:bg-accent hover:border-primary/30 transition-all text-sm group">
                <div class="flex items-center gap-2 mb-0.5">
                  <span class="text-base">{{ cmd.emoji }}</span>
                  <span class="font-semibold text-foreground group-hover:text-primary transition-colors">{{ cmd.label }}</span>
                </div>
                <p class="text-xs text-muted-foreground pl-7">{{ cmd.description }}</p>
              </button>
            }

            <div class="pt-3 border-t border-border">
              <h4 class="text-xs font-semibold text-muted-foreground mb-2">USER ID</h4>
              <input type="number" [(ngModel)]="userId"
                class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/40" />
            </div>
          </div>
        </div>

        <!-- Chat Area -->
        <div class="lg:col-span-3">
          <div class="rounded-2xl border border-border bg-card shadow-sm flex flex-col" style="height: 600px;">
            <!-- Messages -->
            <div class="flex-1 overflow-y-auto p-5 space-y-4" #chatContainer>
              @if (messages.length === 0) {
                <div class="flex flex-col items-center justify-center h-full text-center">
                  <div class="text-6xl mb-4">🤖</div>
                  <h3 class="text-lg font-semibold mb-2">Hello! I'm your AI Assistant</h3>
                  <p class="text-sm text-muted-foreground max-w-md">
                    I can help you borrow/return equipment, generate quizzes, and check your status.
                    Try a quick command or type your request below.
                  </p>
                  <div class="mt-4 flex flex-wrap gap-2 justify-center">
                    @for (ex of exampleCommands; track ex) {
                      <button
                        (click)="sendQuickCommand(ex)"
                        class="px-3 py-1.5 rounded-full text-xs font-medium border border-input bg-background hover:bg-accent transition-colors">
                        {{ ex }}
                      </button>
                    }
                  </div>
                </div>
              }

              @for (msg of messages; track msg.timestamp) {
                <div [class]="msg.role === 'user' ? 'flex justify-end' : 'flex justify-start'">
                  <div [class]="msg.role === 'user'
                    ? 'max-w-[75%] px-4 py-3 rounded-2xl rounded-br-md bg-gradient-to-r from-indigo-500 to-purple-600 text-white'
                    : 'max-w-[75%] px-4 py-3 rounded-2xl rounded-bl-md bg-muted/50 border border-border'">
                    @if (msg.role === 'assistant' && msg.type) {
                      <div class="flex items-center gap-1.5 mb-1.5">
                        <span class="text-xs px-2 py-0.5 rounded-full font-semibold"
                          [class]="msg.type === 'ACTION' ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300'
                            : msg.type === 'ERROR' ? 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300'
                            : msg.type === 'QUIZ_START' ? 'bg-violet-100 text-violet-700 dark:bg-violet-900/30 dark:text-violet-300'
                            : 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300'">
                          {{ msg.type }}
                        </span>
                      </div>
                    }
                    <p class="text-sm whitespace-pre-wrap leading-relaxed">{{ msg.content }}</p>
                    <p class="text-[10px] mt-1.5 opacity-60">
                      {{ msg.timestamp | date:'HH:mm' }}
                    </p>
                  </div>
                </div>
              }

              @if (isSending()) {
                <div class="flex justify-start">
                  <div class="px-4 py-3 rounded-2xl rounded-bl-md bg-muted/50 border border-border">
                    <div class="flex items-center gap-1.5">
                      <div class="w-2 h-2 rounded-full bg-indigo-500 animate-bounce" style="animation-delay: 0ms"></div>
                      <div class="w-2 h-2 rounded-full bg-indigo-500 animate-bounce" style="animation-delay: 150ms"></div>
                      <div class="w-2 h-2 rounded-full bg-indigo-500 animate-bounce" style="animation-delay: 300ms"></div>
                    </div>
                  </div>
                </div>
              }
            </div>

            <!-- Input -->
            <div class="border-t border-border p-4">
              <div class="flex gap-3">
                <input type="text"
                  [(ngModel)]="commandInput"
                  (keyup.enter)="sendCommand()"
                  [disabled]="isSending()"
                  placeholder="Type a command... (e.g. 'quiz sur la géographie' or 'statut')"
                  class="flex-1 px-4 py-3 rounded-xl border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/40 transition-shadow" />
                <button
                  (click)="sendCommand()"
                  [disabled]="!commandInput.trim() || isSending()"
                  class="px-5 py-3 rounded-xl text-sm font-bold text-white bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 disabled:opacity-50 transition-all shadow-lg shadow-indigo-500/25">
                  Send
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class AiVoiceAssistantComponent implements AfterViewChecked {
  @ViewChild('chatContainer') private chatContainer!: ElementRef;
  private readonly aiService: AssistantAIService;

  commandInput = '';
  userId = 1;
  messages: ChatMessage[] = [];
  isSending = signal(false);

  quickCommands = [
    { command: 'statut', label: 'Status', emoji: '📊', description: 'View quiz scores & active loans' },
    { command: 'quiz sur la mémoire', label: 'Memory Quiz', emoji: '🧠', description: 'Generate a memory quiz' },
    { command: 'emprunter fauteuil roulant', label: 'Borrow', emoji: '🦽', description: 'Borrow a wheelchair' },
    { command: 'rendre fauteuil roulant', label: 'Return', emoji: '↩️', description: 'Return equipment' },
  ];

  exampleCommands = [
    'statut',
    'quiz sur les couleurs',
    'emprunter stéthoscope',
  ];

  constructor(aiService: AssistantAIService) {
    this.aiService = aiService;
  }

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  sendCommand(): void {
    if (!this.commandInput.trim() || this.isSending()) return;
    this.doSend(this.commandInput.trim());
    this.commandInput = '';
  }

  sendQuickCommand(command: string): void {
    if (this.isSending()) return;
    this.doSend(command);
  }

  private doSend(command: string): void {
    this.messages.push({
      role: 'user',
      content: command,
      timestamp: new Date(),
    });

    this.isSending.set(true);

    this.aiService.sendVoiceCommand({ command, userId: this.userId }).subscribe({
      next: (res: VoiceCommandResponse) => {
        this.messages.push({
          role: 'assistant',
          content: res.message,
          type: res.type,
          data: res.data,
          timestamp: new Date(),
        });
        this.isSending.set(false);
      },
      error: (err) => {
        this.messages.push({
          role: 'assistant',
          content: err.error?.message || 'Connection error. Is assistant-service running?',
          type: 'ERROR',
          timestamp: new Date(),
        });
        this.isSending.set(false);
      },
    });
  }

  private scrollToBottom(): void {
    try {
      this.chatContainer?.nativeElement?.scrollTo({
        top: this.chatContainer.nativeElement.scrollHeight,
        behavior: 'smooth',
      });
    } catch (_) {}
  }
}
