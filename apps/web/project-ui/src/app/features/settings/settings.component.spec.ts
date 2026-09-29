import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { SettingsComponent } from './settings.component';
import { UserConfigService } from '../../core/services/user-config.service';
import { AuthService } from '../../core/services/auth.service';
import { Observable, of } from 'rxjs';
import {
  AvailableAiModelsResponse,
  UpdateUserConfigRequest,
  UserConfig,
} from '../../core/models/user-config.model';

describe('SettingsComponent', () => {
  let component: SettingsComponent;
  let userConfigServiceMock: {
    getAvailableModels: () => Observable<AvailableAiModelsResponse>;
    updateConfig: (req: UpdateUserConfigRequest) => Observable<UserConfig>;
  };
  let authServiceMock: {
    currentUser: () => {
      id: string;
      username: string;
      active: boolean;
      roles: string[];
      config: UserConfig;
    };
    loadMe: () => Observable<null>;
  };

  const mockAvailableModels: AvailableAiModelsResponse = {
    vendors: ['GEMINI', 'DEEPSEEK'],
    modelsByVendor: {
      GEMINI: [
        { modelId: 'gemini-2.5-flash', displayName: 'Gemini 2.5 Flash', tier: 'CHEAP' },
        { modelId: 'gemini-2.5-pro', displayName: 'Gemini 2.5 Pro', tier: 'STRONG' },
      ],
      DEEPSEEK: [
        { modelId: 'deepseek-chat', displayName: 'DeepSeek V3 (Chat)', tier: 'CHEAP' },
        { modelId: 'deepseek-reasoner', displayName: 'DeepSeek R1 (Reasoner)', tier: 'STRONG' },
      ],
      OLLAMA: [],
    },
  };

  const mockConfig: UserConfig = {
    aiVendor: 'GEMINI',
    cheapModel: 'gemini-2.5-flash',
    strongModel: 'gemini-2.5-pro',
  };

  beforeEach(async () => {
    userConfigServiceMock = {
      getAvailableModels: () => of(mockAvailableModels),
      updateConfig: (req: UpdateUserConfigRequest) => of(req as UserConfig),
    };

    authServiceMock = {
      currentUser: () => ({
        id: '1',
        username: 'testuser',
        active: true,
        roles: ['ROLE_USER'],
        config: mockConfig,
      }),
      loadMe: () => of(null),
    };

    await TestBed.configureTestingModule({
      imports: [SettingsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: UserConfigService, useValue: userConfigServiceMock },
        { provide: AuthService, useValue: authServiceMock },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(SettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load initial config', () => {
    expect(component).toBeTruthy();
    expect(component.form.value.aiVendor).toBe('GEMINI');
    expect(component.form.value.cheapModel).toBe('gemini-2.5-flash');
    expect(component.form.value.strongModel).toBe('gemini-2.5-pro');
  });

  it('should filter available models when vendor changes', () => {
    component.selectVendor('DEEPSEEK');
    expect(component.form.value.aiVendor).toBe('DEEPSEEK');
    expect(component.availableCheapModels().length).toBe(1);
    expect(component.availableCheapModels()[0].modelId).toBe('deepseek-chat');
    expect(component.availableStrongModels()[0].modelId).toBe('deepseek-reasoner');
  });
});
