import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApplicationService } from '../../../core/services/application.service';
import { Application } from '../../../core/models/application.model';

@Component({
  selector: 'app-team-applications',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './team-applications.component.html',
  styleUrl: './team-applications.component.scss',
})
export class TeamApplicationsComponent implements OnInit {
  applications: Application[] = [];
  loading = true;
  error: string | null = null;

  // Group by type for better overview
  byType: Map<string, Application[]> = new Map();

  constructor(private appService: ApplicationService) {}

  ngOnInit(): void {
    this.appService.getLeadApplications().subscribe({
      next: (data) => {
        this.applications = data;
        this.groupByType(data);
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load team applications.';
        this.loading = false;
      },
    });
  }

  private groupByType(apps: Application[]): void {
    this.byType = new Map();
    apps.forEach((app) => {
      const key = `${app.typeName} (${app.typeCode})`;
      if (!this.byType.has(key)) {
        this.byType.set(key, []);
      }
      this.byType.get(key)!.push(app);
    });
  }

  get typeKeys(): string[] {
    return Array.from(this.byType.keys());
  }
}
