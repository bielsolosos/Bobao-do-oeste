export type ModelVendor = 'GEMINI' | 'DEEPSEEK' | 'OLLAMA';

export type ModelTier = 'CHEAP' | 'STRONG';

export interface LlmModelOption {
  modelId: string;
  displayName: string;
  tier: ModelTier;
}

export interface UserConfig {
  aiVendor: ModelVendor;
  cheapModel: string;
  strongModel: string;
}

export interface AvailableAiModelsResponse {
  vendors: ModelVendor[];
  modelsByVendor: Record<string, LlmModelOption[]>;
}

export interface UpdateUserConfigRequest {
  aiVendor: ModelVendor;
  cheapModel: string;
  strongModel: string;
}
