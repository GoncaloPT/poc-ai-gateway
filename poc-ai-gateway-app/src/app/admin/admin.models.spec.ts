import { AdminTool, currentRules, isGranted, needsGrant } from './admin.models';

const tool = (overrides: Partial<AdminTool> = {}): AdminTool => ({
  id: 'svc/list',
  upstream: 'svc',
  internalName: 'list',
  publicName: 'x_list',
  status: 'ENABLED',
  clients: [],
  description: 'List',
  upstreamDescription: 'List',
  descriptionOverride: null,
  readOnly: true,
  destructive: false,
  ...overrides,
});

describe('isGranted', () => {
  it('grants enabled tools without client list to everybody', () => {
    expect(isGranted(tool(), 'any')).toBe(true);
  });

  it('grants only listed clients', () => {
    const granted = tool({ clients: ['chat'] });
    expect(isGranted(granted, 'chat')).toBe(true);
    expect(isGranted(granted, 'ide')).toBe(false);
  });

  it('never grants tools that are not enabled', () => {
    expect(isGranted(tool({ status: 'DISCOVERED' }), 'any')).toBe(false);
    expect(isGranted(tool({ status: 'DISABLED' }), 'any')).toBe(false);
  });
});

describe('currentRules', () => {
  it('keeps status, grants and description override', () => {
    expect(currentRules(tool({ status: 'DISABLED', clients: ['chat'], descriptionOverride: 'Custom' }))).toEqual({
      publicName: 'x_list',
      status: 'DISABLED',
      clients: ['chat'],
      description: 'Custom',
    });
  });
});

describe('needsGrant', () => {
  it('is true for write tools without grants only', () => {
    expect(needsGrant(tool({ readOnly: false, clients: [] }))).toBe(true);
    expect(needsGrant(tool({ readOnly: null, clients: [] }))).toBe(true);
    expect(needsGrant(tool({ readOnly: false, clients: ['chat'] }))).toBe(false);
    expect(needsGrant(tool({ readOnly: true, clients: [] }))).toBe(false);
  });
});
