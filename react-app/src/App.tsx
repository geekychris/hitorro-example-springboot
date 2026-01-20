import { useState } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import './App.css';
import DMSPageEnhanced from './pages/DMSPageEnhanced';
import CrawlerPage from './pages/CrawlerPage';
import TypeSystemPage from './pages/TypeSystemPage';
import CommandsPage from './pages/CommandsPage';
import RestExplorerPage from './pages/RestExplorerPage';
import ServicesExplorerPage from './pages/ServicesExplorerPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

type TabId = 'dms' | 'crawler' | 'types' | 'commands' | 'rest' | 'services';

interface Tab {
  id: TabId;
  label: string;
  description: string;
}

const tabs: Tab[] = [
  { id: 'dms', label: 'Document Management', description: 'Manage documents, containers, versions, and content (includes transformer)' },
  { id: 'crawler', label: 'Filesystem Crawler', description: 'Import files and directories into DMS' },
  { id: 'types', label: 'Type System', description: 'JSON Type System enrichment and field exploration' },
  { id: 'commands', label: 'Commands', description: 'Execute CommandDef annotated methods' },
  { id: 'rest', label: 'REST API Explorer', description: 'Discover and test REST endpoints with streaming support' },
  { id: 'services', label: 'Services', description: 'Explore Hitorro services and dependency hierarchy' },
];

function App() {
  const [activeTab, setActiveTab] = useState<TabId>('dms');

  const renderTabContent = () => {
    switch (activeTab) {
      case 'dms':
        return <DMSPageEnhanced />;
      case 'crawler':
        return <CrawlerPage />;
      case 'types':
        return <TypeSystemPage />;
      case 'commands':
        return <CommandsPage />;
      case 'rest':
        return <RestExplorerPage />;
      case 'services':
        return <ServicesExplorerPage />;
      default:
        return <div>Select a tab</div>;
    }
  };

  return (
    <QueryClientProvider client={queryClient}>
      <div className="app">
        <header className="app-header">
          <h1>Hitorro Test Application</h1>
          <p>Spring Boot Example - Interactive Testing Interface</p>
        </header>
        
        <nav className="nav-tabs">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              className={`nav-tab ${activeTab === tab.id ? 'active' : ''}`}
              onClick={() => setActiveTab(tab.id)}
            >
              {tab.label}
            </button>
          ))}
        </nav>

        <main className="app-content">
          {renderTabContent()}
        </main>
      </div>
    </QueryClientProvider>
  );
}

export default App;
