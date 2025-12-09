export interface ChatRequest {
  message: string;
  conversationId?: number;
}

export interface ChatResponse {
  message: string;
  conversationId: number;
  citations: Citation[];
  timestamp: Date;
}

export interface Citation {
  chunkId: number;
  content: string;
  similarityScore: number;
  metadata: CitationMetadata;
}

export interface CitationMetadata {
  page?: number;
  section?: string;
  documentName: string;
  documentId: number;
}

export interface Conversation {
  id: number;
  title: string;
  createdAt: Date;
  messageCount: number;
}

export interface Message {
  id: number;
  role: 'USER' | 'ASSISTANT';
  content: string;
  createdAt: Date;
  citations?: Citation[];
}
