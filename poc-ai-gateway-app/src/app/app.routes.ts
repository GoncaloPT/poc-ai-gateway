import { Routes } from '@angular/router';
import { ClientsPage } from './clients/clients-page';
import { ToolsPage } from './tools/tools-page';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'tools' },
  { path: 'tools', component: ToolsPage, title: 'Tools - AI Gateway admin' },
  { path: 'clients', component: ClientsPage, title: 'Clients - AI Gateway admin' },
  { path: '**', redirectTo: 'tools' },
];
