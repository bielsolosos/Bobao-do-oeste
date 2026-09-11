import { TestBed } from '@angular/core/testing';
import { UiButtonComponent } from './ui-button.component';

describe('UiButtonComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [UiButtonComponent] }).compileComponents();
  });

  it('aplica as classes da variante primária', () => {
    const fixture = TestBed.createComponent(UiButtonComponent);
    const component = fixture.componentInstance;
    component.variant = 'primary';
    expect(component.getButtonClasses()).toContain('bg-brand-amber-strong');
  });

  it('desabilita o botão durante o loading', () => {
    const fixture = TestBed.createComponent(UiButtonComponent);
    const component = fixture.componentInstance;
    component.loading = true;
    fixture.detectChanges();
    const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    expect(button.getAttribute('aria-busy')).toBe('true');
  });
});
