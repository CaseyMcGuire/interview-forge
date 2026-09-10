import 'graphiql/setup-workers/vite';
import { createGraphiQLFetcher } from '@graphiql/toolkit';
import * as React from 'react';
import { GraphiQL } from 'graphiql';
import { renderComponent } from '@spa-kit/react';
import 'graphiql/style.css';
import CsrfUtils from "utils/CsrfUtils";

function GraphiQLPage() {
  const fetcher = createGraphiQLFetcher({
    url: window.location.origin + '/graphql',
    headers: {
      'X-XSRF-TOKEN': CsrfUtils.getToken(),
    },
  });
  return (
    <GraphiQL fetcher={fetcher} />
  )
}

renderComponent(
  <GraphiQLPage />
);