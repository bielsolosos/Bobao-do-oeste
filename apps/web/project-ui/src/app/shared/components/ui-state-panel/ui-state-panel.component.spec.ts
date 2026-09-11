import { TestBed } from '@angular/core/testing';
import { UiStatePanelComponent } from './ui-state-panel.component';

describe('UiStatePanelComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [UiStatePanelComponent] }).compileComponents();
  });

  it('mostra a ação de retry apenas no estado de erro', () => {
    const fixture = TestBed.createComponent(UiStatePanelComponent);
    fixture.componentRef.setInput('variant', 'empty');
    fixture.componentRef.setInput('title', 'Nada por aqui');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button')).toBeNull();

    fixture.componentRef.setInput('variant', 'error');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button')).toBeTruthy();
  });

  it('emite retry ao clicar', () => {
    const fixture = TestBed.createComponent(UiStatePanelComponent);
    fixture.componentRef.setInput('variant', 'error');
    let emitted = false;
    fixture.componentInstance.retry.subscribe(() => (emitted = true));
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click();
    expect(emitted).toBe(true);
  });
});
