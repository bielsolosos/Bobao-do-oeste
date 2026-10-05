import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserInviteResponse, UserInviteStatus } from '../../../core/models/user-invite.model';
import { InviteService } from '../../../core/services/invite.service';
import { BadgeVariant, UiBadgeComponent } from '../../../shared/components/ui-badge/ui-badge.component';
import { UiButtonComponent } from '../../../shared/components/ui-button/ui-button.component';
import { UiCardComponent } from '../../../shared/components/ui-card/ui-card.component';
import { UiPageHeaderComponent } from '../../../shared/components/ui-page-header/ui-page-header.component';
import { UiPaginationComponent } from '../../../shared/components/ui-pagination/ui-pagination.component';
import { UiStatePanelComponent } from '../../../shared/components/ui-state-panel/ui-state-panel.component';
import { UiToastService } from '../../../shared/components/ui-toast/ui-toast.service';

type ListState = 'loading' | 'error' | 'ready';

@Component({
  selector: 'app-invites-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    UiCardComponent,
    UiBadgeComponent,
    UiButtonComponent,
    UiPaginationComponent,
    UiStatePanelComponent,
    UiPageHeaderComponent,
  ],
  templateUrl: './invites-list.component.html',
})
export class InvitesListComponent implements OnInit {
  private fb = inject(FormBuilder);
  private inviteService = inject(InviteService);
  private toast = inject(UiToastService);

  state = signal<ListState>('loading');
  invites = signal<UserInviteResponse[]>([]);
  showCreateForm = signal(false);
  isCreating = signal(false);

  page = signal(0);
  pageSize = signal(10);
  totalElements = signal(0);
  totalPages = signal(0);

  createForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    role: ['ROLE_USER', Validators.required],
  });

  ngOnInit(): void {
    this.loadInvites();
  }

  loadInvites(): void {
    this.state.set('loading');
    this.inviteService.listInvites(this.page(), this.pageSize()).subscribe({
      next: (res) => {
        this.invites.set(res.content);
        this.totalElements.set(res.totalElements);
        this.totalPages.set(res.totalPages);
        this.state.set('ready');
      },
      error: () => {
        this.state.set('error');
        this.toast.error('Erro', 'Não foi possível carregar os convites.');
      },
    });
  }

  toggleCreateForm(): void {
    this.showCreateForm.update((prev) => !prev);
    if (!this.showCreateForm()) {
      this.createForm.reset({ role: 'ROLE_USER' });
    }
  }

  onCreateInvite(): void {
    this.createForm.markAllAsTouched();
    if (this.createForm.invalid) return;

    this.isCreating.set(true);
    const { email, role } = this.createForm.value;

    this.inviteService
      .createInvite({
        email: email!.trim(),
        role: role!,
      })
      .subscribe({
        next: () => {
          this.isCreating.set(false);
          this.toast.success('Convite enviado!', `O e-mail com o link de acesso foi despachado para ${email}.`);
          this.toggleCreateForm();
          this.loadInvites();
        },
        error: (err) => {
          this.isCreating.set(false);
          this.toast.error('Falha ao enviar', err.error?.message || 'Não foi possível criar o convite.');
        },
      });
  }

  resendInvite(id: string): void {
    this.inviteService.resendInvite(id).subscribe({
      next: () => {
        this.toast.success('Convite renovado', 'Novo e-mail de acesso despachado.');
        this.loadInvites();
      },
      error: (err) => {
        this.toast.error('Erro', err.error?.message || 'Não foi possível reenviar o convite.');
      },
    });
  }

  cancelInvite(id: string): void {
    this.inviteService.cancelInvite(id).subscribe({
      next: () => {
        this.toast.success('Cancelado', 'Convite cancelado com sucesso.');
        this.loadInvites();
      },
      error: (err) => {
        this.toast.error('Erro', err.error?.message || 'Não foi possível cancelar o convite.');
      },
    });
  }

  onPageChange(newPage: number): void {
    this.page.set(newPage);
    this.loadInvites();
  }

  onPageSizeChange(newSize: number): void {
    this.pageSize.set(newSize);
    this.page.set(0);
    this.loadInvites();
  }

  getStatusVariant(status: UserInviteStatus): BadgeVariant {
    switch (status) {
      case 'ACCEPTED':
        return 'success';
      case 'PENDING':
        return 'warning';
      case 'CANCELLED':
        return 'danger';
      case 'EXPIRED':
      default:
        return 'neutral';
    }
  }

  getStatusLabel(status: UserInviteStatus): string {
    switch (status) {
      case 'ACCEPTED':
        return 'Aceito';
      case 'PENDING':
        return 'Pendente';
      case 'CANCELLED':
        return 'Cancelado';
      case 'EXPIRED':
        return 'Expirado';
      default:
        return status;
    }
  }
}
