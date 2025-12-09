import { Component, signal, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ChatService } from '../../../core/services/chat.service';
import { AuthService } from '../../../core/services/auth.service';
import { Message, ChatRequest } from '../../../shared/models/chat.model';
import { User } from '../../../shared/models/user.model';

@Component({
  selector: 'app-chat-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="flex flex-col h-screen bg-gray-50">
      <!-- Header -->
      <nav class="bg-white shadow-sm">
        <div class="max-w-7xl mx-auto px-4 py-4 flex justify-between items-center">
          <div class="flex items-center space-x-4">
            <button
              (click)="router.navigate(['/dashboard'])"
              class="text-blue-600 hover:text-blue-800"
            >
              ← Voltar
            </button>
            <h1 class="text-xl font-bold">Chat RAG</h1>
          </div>
          <span class="text-sm text-gray-600">{{ currentUser()?.name }}</span>
        </div>
      </nav>

      <!-- Messages -->
      <div #messagesContainer class="flex-1 overflow-y-auto p-4 space-y-4">
        @if (messages().length === 0) {
          <div class="text-center text-gray-500 mt-20">
            <p class="text-lg">Faça uma pergunta sobre seus documentos</p>
          </div>
        }

        @for (msg of messages(); track msg.id) {
          <div [class]="msg.role === 'USER' ? 'flex justify-end' : 'flex justify-start'">
            <div [class]="msg.role === 'USER'
              ? 'bg-blue-600 text-white rounded-lg px-4 py-2 max-w-2xl'
              : 'bg-white rounded-lg px-4 py-2 max-w-2xl shadow'">
              <p class="text-sm">{{ msg.content }}</p>

              @if (msg.citations && msg.citations.length > 0) {
                <div class="mt-2 pt-2 border-t border-gray-200">
                  <p class="text-xs text-gray-600 mb-1">Fontes:</p>
                  @for (cite of msg.citations; track cite.chunkId) {
                    <div class="text-xs bg-gray-50 p-2 rounded mt-1">
                      <p class="font-medium">{{ cite.metadata.documentName }}</p>
                      <p class="text-gray-600">Similaridade: {{ (cite.similarityScore * 100).toFixed(1) }}%</p>
                    </div>
                  }
                </div>
              }
            </div>
          </div>
        }

        @if (sending()) {
          <div class="flex justify-start">
            <div class="bg-white rounded-lg px-4 py-2 shadow">
              <div class="flex space-x-2">
                <div class="w-2 h-2 bg-gray-400 rounded-full animate-bounce"></div>
                <div class="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style="animation-delay: 0.1s"></div>
                <div class="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style="animation-delay: 0.2s"></div>
              </div>
            </div>
          </div>
        }
      </div>

      <!-- Input -->
      <div class="bg-white border-t p-4">
        <form (ngSubmit)="sendMessage()" class="max-w-4xl mx-auto flex gap-2">
          <input
            type="text"
            [(ngModel)]="currentMessage"
            name="message"
            placeholder="Digite sua pergunta..."
            [disabled]="sending()"
            class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <button
            type="submit"
            [disabled]="!currentMessage.trim() || sending()"
            class="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Enviar
          </button>
        </form>
      </div>
    </div>
  `
})
export class ChatPageComponent implements OnInit {
  @ViewChild('messagesContainer') messagesContainer!: ElementRef;

  messages = signal<Message[]>([]);
  currentMessage = '';
  sending = signal(false);
  conversationId = signal<number | undefined>(undefined);
  currentUser = signal<User | null>(null);

  constructor(
    private chatService: ChatService,
    private authService: AuthService,
    public router: Router
  ) {
    this.currentUser = this.authService.currentUser;
  }

  ngOnInit() {
    // Load existing conversation if needed
  }

  sendMessage() {
    if (!this.currentMessage.trim() || this.sending()) return;

    const userMessage: Message = {
      id: Date.now(),
      role: 'USER',
      content: this.currentMessage,
      createdAt: new Date()
    };

    this.messages.update(msgs => [...msgs, userMessage]);
    this.sending.set(true);

    const request: ChatRequest = {
      message: this.currentMessage,
      conversationId: this.conversationId()
    };

    this.currentMessage = '';

    this.chatService.sendMessage(request).subscribe({
      next: (response) => {
        this.conversationId.set(response.conversationId);

        const assistantMessage: Message = {
          id: Date.now() + 1,
          role: 'ASSISTANT',
          content: response.message,
          createdAt: response.timestamp,
          citations: response.citations
        };

        this.messages.update(msgs => [...msgs, assistantMessage]);
        this.sending.set(false);
        this.scrollToBottom();
      },
      error: (error) => {
        alert('Erro ao enviar mensagem: ' + (error.error?.message || 'Erro desconhecido'));
        this.sending.set(false);
      }
    });
  }

  scrollToBottom() {
    setTimeout(() => {
      if (this.messagesContainer) {
        this.messagesContainer.nativeElement.scrollTop =
          this.messagesContainer.nativeElement.scrollHeight;
      }
    }, 100);
  }
}
