import { Component, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { RegisterRequest } from '../../../shared/models/user.model';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
      <div class="max-w-md w-full space-y-8 p-8 bg-white rounded-xl shadow-2xl">
        <div class="text-center">
          <h2 class="text-3xl font-bold text-gray-900">Criar Conta</h2>
          <p class="mt-2 text-sm text-gray-600">Registre-se para começar</p>
        </div>

        <form (ngSubmit)="onSubmit()" class="mt-8 space-y-6">
          @if (errorMessage()) {
            <div class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
              {{ errorMessage() }}
            </div>
          }

          <div class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700">Nome</label>
              <input
                type="text"
                required
                [(ngModel)]="formData.name"
                name="name"
                class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700">Email</label>
              <input
                type="email"
                required
                [(ngModel)]="formData.email"
                name="email"
                class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700">Senha</label>
              <input
                type="password"
                required
                [(ngModel)]="formData.password"
                name="password"
                class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              />
            </div>
          </div>

          <button
            type="submit"
            [disabled]="loading()"
            class="w-full py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50"
          >
            {{ loading() ? 'Registrando...' : 'Registrar' }}
          </button>

          <p class="text-center text-sm text-gray-600">
            Já tem uma conta?
            <a routerLink="/login" class="font-medium text-blue-600 hover:text-blue-500">Entrar</a>
          </p>
        </form>
      </div>
    </div>
  `
})
export class RegisterComponent {
  formData: RegisterRequest = { name: '', email: '', password: '' };
  loading = signal(false);
  errorMessage = signal('');

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit() {
    this.loading.set(true);
    this.authService.register(this.formData).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (error) => {
        this.errorMessage.set(error.error?.message || 'Erro ao registrar');
        this.loading.set(false);
      }
    });
  }
}
