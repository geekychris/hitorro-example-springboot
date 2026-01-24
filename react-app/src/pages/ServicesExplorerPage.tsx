import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import axios from 'axios';

interface ServiceInfo {
  shortName: string;
  description: string;
  className: string;
  initialized: boolean;
  version: string;
  dependentServices: string[];
  dependentServiceInterfaces: string[];
  debugCommands: string[];
  typeManagedClasses: string[];
  uiDirectories: string[];
}

interface ServiceHierarchyNode {
  service: ServiceInfo;
  dependencies: ServiceHierarchyNode[];
  level: number;
}

export default function ServicesExplorerPage() {
  const [selectedService, setSelectedService] = useState<ServiceInfo | null>(null);
  const [viewMode, setViewMode] = useState<'list' | 'hierarchy'>('list');
  const [expandedNodes, setExpandedNodes] = useState<Set<string>>(new Set());

  const { data: services, isLoading, error } = useQuery<ServiceInfo[]>({
    queryKey: ['services'],
    queryFn: async () => {
      const response = await axios.get('/api/services/list');
      return response.data;
    },
    refetchInterval: 5000, // Auto-refresh every 5 seconds
  });

  const buildHierarchy = (services: ServiceInfo[]): ServiceHierarchyNode[] => {
    const serviceMap = new Map<string, ServiceInfo>();
    services.forEach(s => serviceMap.set(s.className, s));

    const buildNode = (service: ServiceInfo, level: number, visited: Set<string>): ServiceHierarchyNode => {
      const node: ServiceHierarchyNode = {
        service,
        dependencies: [],
        level,
      };

      // Avoid infinite loops in case of circular dependencies
      if (visited.has(service.className)) {
        return node;
      }
      
      const newVisited = new Set(visited);
      newVisited.add(service.className);
      
      // Add dependencies (services this service depends on)
      service.dependentServices.forEach(depClassName => {
        const depService = serviceMap.get(depClassName);
        if (depService) {
          node.dependencies.push(buildNode(depService, level + 1, newVisited));
        }
      });

      return node;
    };

    // Build a complete tree showing all services
    // Group into two categories:
    // 1. Services that HAVE dependencies (show them with their deps)
    // 2. Services with NO dependencies (leaf services)
    
    const roots: ServiceHierarchyNode[] = [];
    
    // First, show all services that have dependencies (these are the interesting ones)
    const servicesWithDeps = services.filter(s => s.dependentServices.length > 0);
    servicesWithDeps.forEach(service => {
      roots.push(buildNode(service, 0, new Set()));
    });
    
    // Then, show leaf services (services with no dependencies)
    const leafServices = services.filter(s => s.dependentServices.length === 0);
    leafServices.forEach(service => {
      roots.push(buildNode(service, 0, new Set()));
    });

    return roots;
  };

  const toggleNode = (className: string) => {
    const newExpanded = new Set(expandedNodes);
    if (newExpanded.has(className)) {
      newExpanded.delete(className);
    } else {
      newExpanded.add(className);
    }
    setExpandedNodes(newExpanded);
  };

  const renderHierarchyNode = (node: ServiceHierarchyNode) => {
    const isExpanded = expandedNodes.has(node.service.className);
    const hasDeps = node.dependencies.length > 0;
    const indentPx = node.level * 24;

    return (
      <div key={node.service.className} style={{ marginLeft: `${indentPx}px` }}>
        <div
          onClick={() => {
            setSelectedService(node.service);
            if (hasDeps) toggleNode(node.service.className);
          }}
          style={{
            padding: '0.5rem',
            marginBottom: '0.25rem',
            background: selectedService?.className === node.service.className 
              ? 'var(--primary-light)' 
              : 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: '4px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
          }}
        >
          {hasDeps && (
            <span style={{ 
              fontSize: '0.8rem', 
              color: 'var(--text-secondary)',
              width: '16px',
            }}>
              {isExpanded ? '▼' : '▶'}
            </span>
          )}
          {!hasDeps && <span style={{ width: '16px' }}></span>}
          
          <div style={{ flex: 1 }}>
            <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
              {node.service.shortName}
              {!node.service.initialized && (
                <span style={{ 
                  marginLeft: '0.5rem',
                  fontSize: '0.75rem',
                  color: 'var(--warning)',
                  fontWeight: 'normal',
                }}>
                  (not initialized)
                </span>
              )}
            </div>
            <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
              {node.service.description}
            </div>
          </div>

          {node.dependencies.length > 0 && (
            <span style={{
              fontSize: '0.75rem',
              padding: '0.125rem 0.5rem',
              background: 'var(--primary-light)',
              borderRadius: '12px',
              color: 'var(--primary)',
            }}>
              {node.dependencies.length} dep{node.dependencies.length !== 1 ? 's' : ''}
            </span>
          )}
        </div>

        {isExpanded && node.dependencies.length > 0 && (
          <div style={{ marginLeft: '1rem', marginTop: '0.25rem' }}>
            {node.dependencies.map(dep => renderHierarchyNode(dep))}
          </div>
        )}
      </div>
    );
  };

  if (isLoading) {
    return (
      <div style={{ padding: '2rem', textAlign: 'center' }}>
        <p>Loading services...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ padding: '2rem' }}>
        <div className="alert alert-error">
          <strong>Error loading services:</strong> {(error as Error).message}
        </div>
      </div>
    );
  }

  const hierarchy = services ? buildHierarchy(services) : [];
  const sortedServices = services 
    ? [...services].sort((a, b) => a.shortName.localeCompare(b.shortName))
    : [];

  return (
    <div style={{ padding: '2rem' }}>
      <h2 style={{ marginBottom: '0.5rem' }}>Hitorro Services Explorer</h2>
      <p style={{ color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>
        Explore all Hitorro services loaded in the application, their metadata, and dependency relationships.
      </p>

      <div style={{ 
        display: 'flex', 
        gap: '0.5rem', 
        marginBottom: '1rem',
        alignItems: 'center',
      }}>
        <button
          onClick={() => setViewMode('list')}
          className={viewMode === 'list' ? 'button-primary' : 'button-secondary'}
        >
          📋 List View
        </button>
        <button
          onClick={() => setViewMode('hierarchy')}
          className={viewMode === 'hierarchy' ? 'button-primary' : 'button-secondary'}
        >
          🌲 Dependency Tree
        </button>

        <div style={{ flex: 1 }} />

        <span style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
          {services?.length || 0} service{services?.length !== 1 ? 's' : ''} loaded
        </span>
      </div>

      <div className="grid grid-2" style={{ gap: '1.5rem', alignItems: 'flex-start' }}>
        {/* Left panel - List or Hierarchy */}
        <div>
          <div style={{ 
            background: 'var(--surface)', 
            border: '1px solid var(--border)',
            borderRadius: '8px',
            padding: '1rem',
            maxHeight: '600px',
            overflowY: 'auto',
          }}>
            {viewMode === 'list' ? (
              <div>
                <h4 style={{ marginBottom: '1rem' }}>All Services</h4>
                {sortedServices.map(service => (
                  <div
                    key={service.className}
                    onClick={() => setSelectedService(service)}
                    style={{
                      padding: '0.75rem',
                      marginBottom: '0.5rem',
                      background: selectedService?.className === service.className 
                        ? 'var(--primary-light)' 
                        : 'var(--background)',
                      border: '1px solid var(--border)',
                      borderRadius: '4px',
                      cursor: 'pointer',
                    }}
                  >
                    <div style={{ 
                      display: 'flex', 
                      justifyContent: 'space-between',
                      alignItems: 'center',
                    }}>
                      <div>
                        <div style={{ fontWeight: 600, marginBottom: '0.25rem' }}>
                          {service.shortName}
                          {!service.initialized && (
                            <span style={{ 
                              marginLeft: '0.5rem',
                              fontSize: '0.75rem',
                              color: 'var(--warning)',
                              fontWeight: 'normal',
                            }}>
                              (not initialized)
                            </span>
                          )}
                        </div>
                        <div style={{ 
                          fontSize: '0.85rem', 
                          color: 'var(--text-secondary)',
                        }}>
                          {service.description}
                        </div>
                      </div>
                      
                      {service.dependentServices.length > 0 && (
                        <span style={{
                          fontSize: '0.75rem',
                          padding: '0.125rem 0.5rem',
                          background: 'var(--primary-light)',
                          borderRadius: '12px',
                          color: 'var(--primary)',
                        }}>
                          {service.dependentServices.length} dep{service.dependentServices.length !== 1 ? 's' : ''}
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div>
                <h4 style={{ marginBottom: '1rem' }}>
                  Dependency Hierarchy
                  <button
                    onClick={() => setExpandedNodes(new Set(services?.map(s => s.className) || []))}
                    style={{
                      marginLeft: '1rem',
                      fontSize: '0.8rem',
                      padding: '0.25rem 0.5rem',
                    }}
                    className="button-secondary"
                  >
                    Expand All
                  </button>
                  <button
                    onClick={() => setExpandedNodes(new Set())}
                    style={{
                      marginLeft: '0.5rem',
                      fontSize: '0.8rem',
                      padding: '0.25rem 0.5rem',
                    }}
                    className="button-secondary"
                  >
                    Collapse All
                  </button>
                </h4>
                {hierarchy.map(node => renderHierarchyNode(node))}
              </div>
            )}
          </div>
        </div>

        {/* Right panel - Service Details */}
        <div>
          {selectedService ? (
            <div style={{ 
              background: 'var(--surface)', 
              border: '1px solid var(--border)',
              borderRadius: '8px',
              padding: '1.5rem',
            }}>
              <h3 style={{ marginBottom: '0.5rem' }}>
                {selectedService.shortName}
                {!selectedService.initialized && (
                  <span style={{ 
                    marginLeft: '0.5rem',
                    fontSize: '0.9rem',
                    color: 'var(--warning)',
                    fontWeight: 'normal',
                  }}>
                    (not initialized)
                  </span>
                )}
              </h3>
              <p style={{ 
                color: 'var(--text-secondary)', 
                marginBottom: '1.5rem',
                fontSize: '0.95rem',
              }}>
                {selectedService.description}
              </p>

              <div style={{ marginBottom: '1.5rem' }}>
                <h4 style={{ 
                  fontSize: '0.9rem', 
                  marginBottom: '0.5rem',
                  color: 'var(--text-secondary)',
                }}>
                  Class Name
                </h4>
                <code style={{ 
                  display: 'block',
                  padding: '0.5rem',
                  background: 'var(--background)',
                  borderRadius: '4px',
                  fontSize: '0.85rem',
                  overflowX: 'auto',
                }}>
                  {selectedService.className}
                </code>
              </div>

              {selectedService.version && (
                <div style={{ marginBottom: '1.5rem' }}>
                  <h4 style={{ 
                    fontSize: '0.9rem', 
                    marginBottom: '0.5rem',
                    color: 'var(--text-secondary)',
                  }}>
                    Version
                  </h4>
                  <p>{selectedService.version}</p>
                </div>
              )}

              {selectedService.dependentServices.length > 0 && (
                <div style={{ marginBottom: '1.5rem' }}>
                  <h4 style={{ 
                    fontSize: '0.9rem', 
                    marginBottom: '0.5rem',
                    color: 'var(--text-secondary)',
                  }}>
                    Dependencies ({selectedService.dependentServices.length})
                  </h4>
                  <ul style={{ 
                    margin: 0, 
                    paddingLeft: '1.5rem',
                    fontSize: '0.9rem',
                  }}>
                    {selectedService.dependentServices.map(dep => {
                      const depService = services?.find(s => s.className === dep);
                      return (
                        <li key={dep} style={{ marginBottom: '0.25rem' }}>
                          {depService ? (
                            <button
                              onClick={() => setSelectedService(depService)}
                              style={{
                                background: 'none',
                                border: 'none',
                                color: 'var(--primary)',
                                cursor: 'pointer',
                                textDecoration: 'underline',
                                padding: 0,
                              }}
                            >
                              {depService.shortName}
                            </button>
                          ) : (
                            <span style={{ color: 'var(--text-secondary)' }}>
                              {dep.split('.').pop()}
                            </span>
                          )}
                        </li>
                      );
                    })}
                  </ul>
                </div>
              )}

              {selectedService.dependentServiceInterfaces.length > 0 && (
                <div style={{ marginBottom: '1.5rem' }}>
                  <h4 style={{ 
                    fontSize: '0.9rem', 
                    marginBottom: '0.5rem',
                    color: 'var(--text-secondary)',
                  }}>
                    Service Interfaces ({selectedService.dependentServiceInterfaces.length})
                  </h4>
                  <ul style={{ 
                    margin: 0, 
                    paddingLeft: '1.5rem',
                    fontSize: '0.9rem',
                  }}>
                    {selectedService.dependentServiceInterfaces.map(iface => (
                      <li key={iface} style={{ marginBottom: '0.25rem' }}>
                        {iface.split('.').pop()}
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {selectedService.debugCommands.length > 0 && (
                <div style={{ marginBottom: '1.5rem' }}>
                  <h4 style={{ 
                    fontSize: '0.9rem', 
                    marginBottom: '0.5rem',
                    color: 'var(--text-secondary)',
                  }}>
                    Debug Commands ({selectedService.debugCommands.length})
                  </h4>
                  <div style={{ 
                    display: 'flex', 
                    flexWrap: 'wrap', 
                    gap: '0.5rem',
                  }}>
                    {selectedService.debugCommands.map(cmd => (
                      <span
                        key={cmd}
                        style={{
                          padding: '0.25rem 0.5rem',
                          background: 'var(--background)',
                          border: '1px solid var(--border)',
                          borderRadius: '4px',
                          fontSize: '0.85rem',
                          fontFamily: 'monospace',
                        }}
                      >
                        {cmd}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {selectedService.typeManagedClasses.length > 0 && (
                <div style={{ marginBottom: '1.5rem' }}>
                  <h4 style={{ 
                    fontSize: '0.9rem', 
                    marginBottom: '0.5rem',
                    color: 'var(--text-secondary)',
                  }}>
                    Type-Managed Classes ({selectedService.typeManagedClasses.length})
                  </h4>
                  <ul style={{ 
                    margin: 0, 
                    paddingLeft: '1.5rem',
                    fontSize: '0.9rem',
                  }}>
                    {selectedService.typeManagedClasses.map(cls => (
                      <li key={cls} style={{ marginBottom: '0.25rem' }}>
                        {cls.split('.').pop()}
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {selectedService.uiDirectories.length > 0 && (
                <div style={{ marginBottom: '1.5rem' }}>
                  <h4 style={{ 
                    fontSize: '0.9rem', 
                    marginBottom: '0.5rem',
                    color: 'var(--text-secondary)',
                  }}>
                    UI Directories ({selectedService.uiDirectories.length})
                  </h4>
                  <ul style={{ 
                    margin: 0, 
                    paddingLeft: '1.5rem',
                    fontSize: '0.9rem',
                  }}>
                    {selectedService.uiDirectories.map(dir => (
                      <li key={dir} style={{ marginBottom: '0.25rem' }}>
                        {dir}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          ) : (
            <div style={{ 
              background: 'var(--surface)', 
              border: '1px solid var(--border)',
              borderRadius: '8px',
              padding: '3rem',
              textAlign: 'center',
              color: 'var(--text-secondary)',
            }}>
              <p>Select a service to view details</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
