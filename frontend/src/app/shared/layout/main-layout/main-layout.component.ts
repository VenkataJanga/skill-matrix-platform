import { Component, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { UserMe } from '../../../core/models/auth.model';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './main-layout.component.html',
  styleUrls: ['./main-layout.component.scss'],
})
export class MainLayoutComponent implements OnInit {
  currentUser: UserMe | null = null;
  sidebarOpen = true;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe((user) => {
      this.currentUser = user;
    });
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  logout(): void {
    this.authService.logout();
  }

  get isAdmin(): boolean {
    return this.currentUser?.roles?.includes('ADMIN') ?? false;
  }

  get isLeadManager(): boolean {
    return this.currentUser?.roles?.includes('LEAD_MANAGER') ?? false;
  }

  get isTechnician(): boolean {
    return this.currentUser?.roles?.includes('TECHNICIAN') ?? false;
  }
}
