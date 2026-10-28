import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { TechnicianAssessmentService } from '../../../core/services/technician-assessment.service';
import { TechnicianAssessmentSummary } from '../../../core/models/assessment.model';

@Component({
  selector: 'app-my-assessments',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './my-assessments.component.html',
  styleUrl: './my-assessments.component.scss',
})
export class MyAssessmentsComponent implements OnInit {
  assessments: TechnicianAssessmentSummary[] = [];
  loading = false;
  error: string | null = null;

  constructor(
    private technicianAssessmentService: TechnicianAssessmentService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = null;
    this.technicianAssessmentService.getMyAssessments().subscribe({
      next: (data) => { this.assessments = data; this.loading = false; },
      error: () => { this.error = 'Failed to load your assessments.'; this.loading = false; },
    });
  }

  get openCount(): number { return this.assessments.filter((a) => a.assessmentStatus === 'NOT_STARTED').length; }
  get draftCount(): number { return this.assessments.filter((a) => a.assessmentStatus === 'DRAFT').length; }
  get submittedCount(): number { return this.assessments.filter((a) => a.assessmentStatus === 'SUBMITTED').length; }

  progressLabel(a: TechnicianAssessmentSummary): string {
    if (a.assessmentStatus === 'NOT_STARTED') return '—';
    return `${a.ratedItems} of ${a.totalItems} rated`;
  }

  /**
   * Derived date-window indicator. Purely for UX — the backend remains
   * authoritative for every start/save/submit action regardless of what is
   * displayed here (a stale client clock cannot bypass server-side validation).
   */
  windowState(a: TechnicianAssessmentSummary): 'NOT_YET_OPEN' | 'ACTIVE' | 'EXPIRED' {
    const today = new Date().toISOString().slice(0, 10);
    if (a.cycleStartDate && today < a.cycleStartDate) return 'NOT_YET_OPEN';
    if (a.cycleEndDate && today > a.cycleEndDate) return 'EXPIRED';
    return 'ACTIVE';
  }

  windowLabel(a: TechnicianAssessmentSummary): string | null {
    const state = this.windowState(a);
    if (state === 'NOT_YET_OPEN') return 'Not Yet Open';
    if (state === 'EXPIRED') return 'Expired';
    return null;
  }

  actionLabel(a: TechnicianAssessmentSummary): string {
    const state = this.windowState(a);
    if (a.assessmentStatus === 'SUBMITTED') return 'View';
    if (state === 'EXPIRED') {
      return a.assessmentStatus === 'DRAFT' ? 'View' : 'Unavailable';
    }
    if (state === 'NOT_YET_OPEN') return 'Not Yet Open';
    switch (a.assessmentStatus) {
      case 'NOT_STARTED': return 'Start Assessment';
      case 'DRAFT': return 'Continue';
      default: return 'Open';
    }
  }

  /** Disable the action button entirely before the window opens, or when expired with no draft to view. */
  isActionDisabled(a: TechnicianAssessmentSummary): boolean {
    const state = this.windowState(a);
    if (a.assessmentStatus === 'SUBMITTED') return false;
    if (state === 'NOT_YET_OPEN') return true;
    if (state === 'EXPIRED' && a.assessmentStatus === 'NOT_STARTED') return true;
    return false;
  }

  onAction(a: TechnicianAssessmentSummary): void {
    if (this.isActionDisabled(a)) return;

    if (a.assessmentStatus === 'NOT_STARTED') {
      this.technicianAssessmentService.startAssessment(a.cyclePublicId).subscribe({
        next: (assessment) => this.router.navigate(['/technician/self-assessment', assessment.assessmentPublicId]),
        error: (err) => { this.error = err?.error?.message ?? 'Failed to start assessment.'; },
      });
    } else if (a.assessmentPublicId) {
      this.router.navigate(['/technician/self-assessment', a.assessmentPublicId]);
    }
  }
}
