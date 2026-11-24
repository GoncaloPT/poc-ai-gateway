import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApplicationRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AdminApi } from './admin-api';
import { AdminTool } from './admin.models';

describe('AdminApi', () => {
  let api: AdminApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(AdminApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify({ ignoreCancelled: true }));

  it('loads clients and tools', async () => {
    TestBed.tick();
    http.expectOne('/admin/clients').flush({ chat: true });
    http.expectOne('/admin/tools').flush([]);
    await TestBed.inject(ApplicationRef).whenStable();

    expect(api.clients.value()).toEqual({ chat: true });
    expect(api.tools.value()).toEqual([]);
  });

  it('puts client state', () => {
    api.setClientEnabled('my agent', false).subscribe();

    const req = http.expectOne('/admin/clients/my%20agent');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ enabled: false });
  });

  it('puts tool rules on upstream and internal name', () => {
    const tool = { upstream: 'car-brand-service', internalName: 'remove_car_brand' } as AdminTool;
    api.updateTool(tool, { publicName: 'x_list', status: 'ENABLED', clients: ['chat'], description: null }).subscribe();

    const req = http.expectOne('/admin/tools/car-brand-service/remove_car_brand');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ publicName: 'x_list', status: 'ENABLED', clients: ['chat'], description: null });
  });
});
