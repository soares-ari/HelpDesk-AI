import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Document, DocumentUploadResponse } from '../../shared/models/document.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  private readonly API_URL = `${environment.apiUrl}/documents`;

  constructor(private http: HttpClient) {}

  uploadDocument(file: File): Observable<DocumentUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<DocumentUploadResponse>(`${this.API_URL}/upload`, formData);
  }

  getDocuments(): Observable<Document[]> {
    return this.http.get<Document[]>(this.API_URL);
  }

  getDocument(id: number): Observable<Document> {
    return this.http.get<Document>(`${this.API_URL}/${id}`);
  }

  deleteDocument(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }
}
