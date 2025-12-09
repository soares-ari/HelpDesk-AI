import { Component, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { DocumentService } from '../../../core/services/document.service';
import { Document, DocumentStatus } from '../../../shared/models/document.model';
import { User } from '../../../shared/models/user.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="min-h-screen bg-gray-50">
      <!-- Navbar -->
      <nav class="bg-white shadow-sm">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div class="flex justify-between h-16">
            <div class="flex items-center">
              <h1 class="text-xl font-bold text-gray-900">Helpdesk AI</h1>
            </div>
            <div class="flex items-center space-x-4">
              <span class="text-sm text-gray-700">{{ currentUser()?.name }}</span>
              <button
                (click)="logout()"
                class="px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 rounded-md"
              >
                Sair
              </button>
            </div>
          </div>
        </div>
      </nav>

      <!-- Main Content -->
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <!-- Upload Section -->
        <div class="bg-white rounded-lg shadow p-6 mb-8">
          <h2 class="text-2xl font-bold text-gray-900 mb-4">Upload de Documentos</h2>

          <div
            class="border-2 border-dashed rounded-lg p-8 text-center transition-colors"
            [class.border-blue-500]="isDragging()"
            [class.bg-blue-50]="isDragging()"
            [class.border-gray-300]="!isDragging()"
            [class.hover:border-blue-500]="!isDragging()"
            (dragover)="onDragOver($event)"
            (dragleave)="onDragLeave($event)"
            (drop)="onDrop($event)"
          >
            <input
              type="file"
              #fileInput
              (change)="onFileSelected($event)"
              accept="application/pdf"
              class="hidden"
            />

            @if (uploading()) {
              <div class="text-blue-600">
                <svg class="animate-spin h-12 w-12 mx-auto mb-4" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <p class="text-lg">Enviando documento...</p>
              </div>
            } @else {
              <div (click)="fileInput.click()" class="cursor-pointer">
                <svg class="mx-auto h-12 w-12 text-gray-400" stroke="currentColor" fill="none" viewBox="0 0 48 48">
                  <path d="M28 8H12a4 4 0 00-4 4v20m32-12v8m0 0v8a4 4 0 01-4 4H12a4 4 0 01-4-4v-4m32-4l-3.172-3.172a4 4 0 00-5.656 0L28 28M8 32l9.172-9.172a4 4 0 015.656 0L28 28m0 0l4 4m4-24h8m-4-4v8m-12 4h.02" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
                <p class="mt-2 text-sm text-gray-600">
                  Clique para selecionar ou arraste um arquivo PDF
                </p>
                <p class="text-xs text-gray-500 mt-1">Máximo 50MB</p>
              </div>
            }
          </div>

          @if (uploadError()) {
            <div class="mt-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
              {{ uploadError() }}
            </div>
          }
          @if (uploadSuccess()) {
            <div class="mt-4 bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded">
              {{ uploadSuccess() }}
            </div>
          }
        </div>

        <!-- Documents List -->
        <div class="bg-white rounded-lg shadow p-6">
          <div class="flex justify-between items-center mb-4">
            <h2 class="text-2xl font-bold text-gray-900">Meus Documentos</h2>
            <button
              (click)="router.navigate(['/chat'])"
              class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
            >
              Ir para Chat
            </button>
          </div>

          @if (loadingDocs()) {
            <p class="text-center text-gray-500 py-8">Carregando documentos...</p>
          } @else if (documents().length === 0) {
            <p class="text-center text-gray-500 py-8">Nenhum documento enviado ainda</p>
          } @else {
            <div class="overflow-x-auto">
              <table class="min-w-full divide-y divide-gray-200">
                <thead class="bg-gray-50">
                  <tr>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Arquivo</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Chunks</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Data</th>
                    <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Ações</th>
                  </tr>
                </thead>
                <tbody class="bg-white divide-y divide-gray-200">
                  @for (doc of documents(); track doc.id) {
                    <tr>
                      <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        {{ doc.filename }}
                      </td>
                      <td class="px-6 py-4 whitespace-nowrap">
                        <span [class]="getStatusClass(doc.status)">
                          {{ getStatusText(doc.status) }}
                        </span>
                      </td>
                      <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {{ doc.totalChunks }}
                      </td>
                      <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {{ formatDate(doc.uploadedAt) }}
                      </td>
                      <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                        <button
                          (click)="deleteDocument(doc.id)"
                          class="text-red-600 hover:text-red-900"
                        >
                          Deletar
                        </button>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </div>
      </div>
    </div>
  `
})
export class DashboardComponent implements OnInit {
  documents = signal<Document[]>([]);
  loadingDocs = signal(false);
  uploading = signal(false);
  uploadError = signal('');
  uploadSuccess = signal('');
  currentUser = signal<User | null>(null);
  isDragging = signal(false);

  constructor(
    private documentService: DocumentService,
    private authService: AuthService,
    public router: Router
  ) {
    this.currentUser = this.authService.currentUser;
  }

  ngOnInit() {
    this.loadDocuments();
  }

  loadDocuments() {
    this.loadingDocs.set(true);
    this.documentService.getDocuments().subscribe({
      next: (docs) => {
        this.documents.set(docs);
        this.loadingDocs.set(false);
      },
      error: () => this.loadingDocs.set(false)
    });
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(true);
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(false);
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(false);

    const files = event.dataTransfer?.files;
    if (!files || files.length === 0) return;

    const file = files[0];
    this.processFile(file);
  }

  onFileSelected(event: any) {
    const file: File = event.target.files[0];
    if (!file) return;
    this.processFile(file);
  }

  private processFile(file: File) {
    if (file.type !== 'application/pdf') {
      this.uploadError.set('Apenas arquivos PDF são permitidos');
      return;
    }

    if (file.size > 50 * 1024 * 1024) {
      this.uploadError.set('Arquivo muito grande. Máximo 50MB');
      return;
    }

    this.uploading.set(true);
    this.uploadError.set('');
    this.uploadSuccess.set('');

    this.documentService.uploadDocument(file).subscribe({
      next: (response) => {
        this.uploadSuccess.set('Documento enviado com sucesso!');
        this.uploading.set(false);
        setTimeout(() => this.loadDocuments(), 2000);
      },
      error: (error) => {
        this.uploadError.set(error.error?.message || 'Erro ao enviar documento');
        this.uploading.set(false);
      }
    });
  }

  deleteDocument(id: number) {
    if (!confirm('Tem certeza que deseja deletar este documento?')) return;

    this.documentService.deleteDocument(id).subscribe({
      next: () => this.loadDocuments(),
      error: (error) => alert('Erro ao deletar documento')
    });
  }

  getStatusClass(status: DocumentStatus): string {
    const classes: Record<DocumentStatus, string> = {
      [DocumentStatus.PROCESSING]: 'px-2 py-1 text-xs rounded-full bg-yellow-100 text-yellow-800',
      [DocumentStatus.COMPLETED]: 'px-2 py-1 text-xs rounded-full bg-green-100 text-green-800',
      [DocumentStatus.FAILED]: 'px-2 py-1 text-xs rounded-full bg-red-100 text-red-800'
    };
    return classes[status];
  }

  getStatusText(status: DocumentStatus): string {
    const texts: Record<DocumentStatus, string> = {
      [DocumentStatus.PROCESSING]: 'Processando',
      [DocumentStatus.COMPLETED]: 'Completo',
      [DocumentStatus.FAILED]: 'Falhou'
    };
    return texts[status];
  }

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('pt-BR');
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
