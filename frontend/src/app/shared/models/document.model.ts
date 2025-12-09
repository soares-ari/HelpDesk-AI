export enum DocumentStatus {
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED'
}

export interface Document {
  id: number;
  filename: string;
  fileSize: number;
  mimeType: string;
  status: DocumentStatus;
  totalChunks: number;
  uploadedAt: Date;
  userId: number;
}

export interface DocumentUploadResponse {
  documentId: number;
  filename: string;
  status: string;
  message: string;
}
