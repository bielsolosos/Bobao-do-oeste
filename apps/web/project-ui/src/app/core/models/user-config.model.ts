export type ModelVendor = 'GEMINI' | 'DEEPSEEK';

export interface ModelOptionDto {
  id: string;
  name: string;
  description: string;
}

export interface VendorModelsDto {
  vendor: ModelVendor;
  displayName: string;
  cheapModels: ModelOptionDto[];
  strongModels: ModelOptionDto[];
}

export interface AvailableAiModelsResponse {
  vendors: VendorModelsDto[];
}

export interface UserConfig {
  aiVendor: ModelVendor;
  cheapModel: string;
  strongModel: string;
}

export interface UpdateUserConfigRequest {
  aiVendor: ModelVendor;
  cheapModel: string;
  strongModel: string;
}
