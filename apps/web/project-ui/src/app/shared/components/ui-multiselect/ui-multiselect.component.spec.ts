import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Component } from '@angular/core';
import { UiMultiSelectComponent, MultiSelectOption } from './ui-multiselect.component';

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, UiMultiSelectComponent],
  template: `
    <app-ui-multiselect
      [formControl]="control"
      [options]="options"
      placeholder="Selecione as opções"
    ></app-ui-multiselect>
  `,
})
class TestHostComponent {
  control = new FormControl<string[]>([]);
  options: MultiSelectOption<string>[] = [
    { label: 'Opção 1', value: 'OPT1', description: 'Desc 1' },
    { label: 'Opção 2', value: 'OPT2', description: 'Desc 2' },
    { label: 'Opção 3', value: 'OPT3', description: 'Desc 3' },
  ];
}

describe('UiMultiSelectComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let host: TestHostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    host = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve inicializar com valor vazio', () => {
    expect(host.control.value).toEqual([]);
  });

  it('deve atualizar o form control quando o valor é alterado programaticamente', () => {
    host.control.setValue(['OPT1', 'OPT2']);
    fixture.detectChanges();
    expect(host.control.value).toEqual(['OPT1', 'OPT2']);
  });
});
