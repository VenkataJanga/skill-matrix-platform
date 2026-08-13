import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApplicationService } from '../../../core/services/application.service';
import { Application } from '../../../core/models/application.model';

@Component({
  selector: 'app-my-applications',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './my-applications.component.html',
  styleUrl: './my-applications.component.scss',
})
export class MyApplicationsComponent implements OnInit {
  applications: Application[] = [];
  loading = true;
  error: string | null = null;

  constructor(private appService: ApplicationService) {}

  ngOnInit(): void {
    this.appService.getMyApplications().subscribe({
      next: (data) => {
        this.applications = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load your applications.';
        this.loading = false;
      },
    });
  }
}
