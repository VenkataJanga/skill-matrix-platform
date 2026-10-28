import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TechnicianAssessmentService } from '../../../core/services/technician-assessment.service';
import { TechnicianAssessment, TechnicianAssessmentItem } from '../../../core/models/assessment.model';

const RATING_LABELS: Record<number, string> = {
  1: 'Awareness',
  2: 'Working Knowledge',
  3: 'Independent Contributor',
  4: 'Advanced Practitioner',
  5: 'SME / Expert',
};

@Component({
  selector: 'app-self-assessment',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './self-assessment.component.html',
  styleUrl: './self-assessment.component.scss',
})
export class SelfAssessmentComponent implements OnInit {
  assessment: TechnicianAssessment | null = null;
  loading = false;
  error: string | null = null;
  successMessage: string | null = null;
  saving = false;
  submitting = false;
  showSubmitConfirm = false;

  ratings = [1, 2, 3, 4, 5];
  ratingLabels = RATING_LABELS;

  assessmentPublicId!: string;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private technicianAssessmentService: TechnicianAssessmentService
  ) {}

  ngOnInit(): void {
    this.assessmentPublicId = this.route.snapshot.paramMap.get('publicId')!;
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = null;
    this.technicianAssessmentService.getAssessment(this.assessmentPublicId).subscribe({
      next: (a) => { this.assessment = a; this.loading = false; },
      error: (err) => {
        this.error = err?.error?.message ?? 'Failed to load assessment.';
        this.loading = false;
      },
    });
  }

  get isReadOnly(): boolean {
    return this.assessment?.statusCode === 'SUBMITTED' || this.isCycleExpired;
  }

  /**
   * True when the cycle's end date has passed (client-side UX hint only).
   * The backend independently and authoritatively rejects saveDraft/submit
   * once the date window closes, regardless of what this getter returns.
   */
  get isCycleExpired(): boolean {
    if (!this.assessment?.cycleEndDate) return false;
    const today = new Date().toISOString().slice(0, 10);
    return today > this.assessment.cycleEndDate;
  }

  groupedItems(): { group: string; items: TechnicianAssessmentItem[] }[] {
    if (!this.assessment) return [];
    const groups = new Map<string, TechnicianAssessmentItem[]>();
    for (const item of this.assessment.items) {
      const key = item.skillGroup ?? item.categoryName ?? 'Other';
      if (!groups.has(key)) groups.set(key, []);
      groups.get(key)!.push(item);
    }
    return Array.from(groups.entries()).map(([group, items]) => ({ group: this.groupLabel(group), items }));
  }

  private groupLabel(code: string): string {
    switch (code) {
      case 'COMMON_TECH': return 'Common Technical Skills';
      case 'APP_SPECIFIC': return 'Application-Specific Skills';
      case 'VW_TOOL': return 'VW-Specific Tools';
      default: return code;
    }
  }

  get mandatoryTotal(): number {
    return this.assessment?.items.filter((i) => i.mandatory).length ?? 0;
  }

  get mandatoryCompleted(): number {
    return this.assessment?.items.filter((i) => i.mandatory && i.selfRating != null).length ?? 0;
  }

  get canSubmit(): boolean {
    return this.mandatoryCompleted === this.mandatoryTotal && this.mandatoryTotal > 0;
  }

  saveDraft(): void {
    if (!this.assessment || this.isReadOnly) return;
    this.saving = true;
    this.error = null;
    const req = {
      version: this.assessment.version ?? 0,
      items: this.assessment.items.map((i) => ({
        assessmentItemPublicId: i.assessmentItemPublicId,
        selfRating: i.selfRating,
        comment: i.comment,
        evidence: i.evidence,
      })),
    };
    this.technicianAssessmentService.saveDraft(this.assessmentPublicId, req).subscribe({
      next: (a) => {
        this.assessment = a;
        this.successMessage = 'Draft saved.';
        this.saving = false;
      },
      error: (err) => {
        this.error = err?.error?.message ?? 'Failed to save draft. Please refresh and try again.';
        this.saving = false;
      },
    });
  }

  requestSubmit(): void {
    if (!this.canSubmit) return;
    this.showSubmitConfirm = true;
  }

  cancelSubmit(): void { this.showSubmitConfirm = false; }

  confirmSubmit(): void {
    this.showSubmitConfirm = false;
    this.submitting = true;
    this.error = null;
    this.technicianAssessmentService.submit(this.assessmentPublicId).subscribe({
      next: (a) => {
        this.assessment = a;
        this.successMessage = 'Assessment submitted successfully.';
        this.submitting = false;
      },
      error: (err) => {
        this.error = err?.error?.message ?? 'Failed to submit assessment.';
        this.submitting = false;
      },
    });
  }

  backToList(): void {
    this.router.navigate(['/technician/my-assessments']);
  }

  dismissSuccess(): void { this.successMessage = null; }
  dismissError(): void { this.error = null; }
}
