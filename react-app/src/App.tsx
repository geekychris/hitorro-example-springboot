import { useState } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import './App.css';
import DMSPageEnhanced from './pages/DMSPageEnhanced';
import CrawlerPage from './pages/CrawlerPage';
import TypeSystemPage from './pages/TypeSystemPage';
import CommandsPage from './pages/CommandsPage';
import RestExplorerPage from './pages/RestExplorerPage';
import ServicesExplorerPage from './pages/ServicesExplorerPage';
import StructuredLoggingPage from './pages/StructuredLoggingPage';
import SearchPage from './pages/SearchPage';
import LuceneViewerPage from './pages/LuceneViewerPage';
import DataMapperPage from './pages/DataMapperPage';
import GitToolsPage from './pages/GitToolsPage';
import PlaygroundPage from './pages/PlaygroundPage';
import JvsSqlPage from './pages/JvsSqlPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

type TabId = 'dms' | 'crawler' | 'types' | 'datamapper' | 'playground' | 'jvssql' | 'commands' | 'rest' | 'services' | 'logging' | 'search' | 'luceneviewer' | 'gittools';

interface Tab {
  id: TabId;
  label: string;
  description: string;
}

const tabs: Tab[] = [
  { id: 'dms', label: 'Document Management', description: 'Manage documents, containers, versions, and content (includes transformer)' },
  { id: 'crawler', label: 'Filesystem Crawler', description: 'Import files and directories into DMS' },
  { id: 'types', label: 'Type System', description: 'JSON Type System enrichment and field exploration' },
  { id: 'datamapper', label: 'Data Mapper', description: 'Groovy DSL transforms with synthetic data generation' },
  { id: 'playground', label: 'JVS Playground', description: 'Interactive showcase for new projection/validation/NLP features' },
  { id: 'jvssql', label: 'JVS SQL', description: 'Streaming SQL over JVS documents — Calcite parser, custom executor, spill sort, joins, windowed aggregation' },
  { id: 'search', label: 'Search', description: 'Lucene-based document search and indexing with facets' },
  { id: 'luceneviewer', label: 'Lucene Viewer', description: 'Luke-like index browser (fields, stored docs, terms, search)' },
  { id: 'commands', label: 'Commands', description: 'Execute CommandDef annotated methods' },
  { id: 'rest', label: 'REST API Explorer', description: 'Discover and test REST endpoints with streaming support' },
  { id: 'services', label: 'Services', description: 'Explore Hitorro services and dependency hierarchy' },
  { id: 'logging', label: 'Structured Logging', description: 'Config-driven structured logging with Kafka integration' },
  { id: 'gittools', label: 'Git Tools', description: 'Repository manager, commit browser, tagging, and PR tools' },
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
      case 'datamapper':
        return <DataMapperPage />;
      case 'playground':
        return <PlaygroundPage />;
      case 'jvssql':
        return <JvsSqlPage />;
      case 'search':
        return <SearchPage />;
      case 'luceneviewer':
        return <LuceneViewerPage />;
      case 'commands':
        return <CommandsPage />;
      case 'rest':
        return <RestExplorerPage />;
      case 'services':
        return <ServicesExplorerPage />;
      case 'logging':
        return <StructuredLoggingPage />;
      case 'gittools':
        return <GitToolsPage />;
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
