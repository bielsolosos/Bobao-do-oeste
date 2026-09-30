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
    vendors: [
      {
        vendor: 'GEMINI',
        displayName: 'Google Gemini',
        cheapModels: [{ id: 'gemini-2.5-flash', name: 'Gemini 2.5 Flash', description: 'Fast' }],
        strongModels: [{ id: 'gemini-2.5-pro', name: 'Gemini 2.5 Pro', description: 'Strong' }],
      },
      {
        vendor: 'DEEPSEEK',
        displayName: 'DeepSeek',
        cheapModels: [{ id: 'deepseek-chat', name: 'DeepSeek V3 (Chat)', description: 'Fast' }],
        strongModels: [
          { id: 'deepseek-reasoner', name: 'DeepSeek R1 (Reasoner)', description: 'Strong' },
        ],
      },
    ],
  };

  const mockConfig: UserConfig = {
    aiVendor: 'GEMINI',
    cheapModel: 'gemini-2.5-flash',
    strongModel: 'gemini-2.5-pro',
    discordWebhookUrl: 'https://discord.com/api/webhooks/123456/tokenABC',
    discordEnabled: true,
    emailEnabled: false,
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

  it('should create and load initial config including discord webhook', () => {
    expect(component).toBeTruthy();
    expect(component.form.value.aiVendor).toBe('GEMINI');
    expect(component.form.value.cheapModel).toBe('gemini-2.5-flash');
    expect(component.form.value.strongModel).toBe('gemini-2.5-pro');
    expect(component.form.value.discordWebhookUrl).toBe(
      'https://discord.com/api/webhooks/123456/tokenABC',
    );
    expect(component.form.value.discordEnabled).toBe(true);
    expect(component.isDiscordConfigured()).toBe(true);
  });

  it('should filter available models when vendor changes', () => {
    component.selectVendor('DEEPSEEK');
    expect(component.form.value.aiVendor).toBe('DEEPSEEK');
    expect(component.availableCheapModels().length).toBe(1);
    expect(component.availableCheapModels()[0].id).toBe('deepseek-chat');
    expect(component.availableStrongModels()[0].id).toBe('deepseek-reasoner');
  });

  it('should clear discord webhook when clearDiscordWebhook is called', () => {
    component.clearDiscordWebhook();
    expect(component.form.value.discordWebhookUrl).toBe('');
    expect(component.isDiscordConfigured()).toBe(false);
  });
});
