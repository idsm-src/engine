window.info = fetch('?info', {
    method: "GET",
    headers: {
      "Accept": "application/json;",
    },
    redirect: "follow",
  }).then(response => response.json());


window.onload = function () {
  var endpoint = window.location.protocol + '//' + window.location.host + window.location.pathname;

  Yasgui.Yasqe.defaults.autocompleters.splice(Yasgui.Yasqe.defaults.autocompleters.indexOf('prefixes'), 1);
  Yasgui.Yasqe.defaults.autocompleters.splice(Yasgui.Yasqe.defaults.autocompleters.indexOf('property'), 1);
  Yasgui.Yasqe.defaults.autocompleters.splice(Yasgui.Yasqe.defaults.autocompleters.indexOf('class'), 1);

  Yasgui.Yasqe.forkAutocompleter("prefixes", {
    name: "prefixes-local",
    persistenceId: null,
    get: () => window.info.then(data => Object.keys(data.prefixes).map(key => key + ": <" + data.prefixes[key] + ">").sort())
      .catch(() => null)
  });

  Yasgui.Yasqe.forkAutocompleter("property", {
    name: "property-local",
    persistenceId: null,
    get: (yasqe, token) => window.info.then(data => data.properties.filter(iri => iri.startsWith(token.autocompletionString)))
      .catch(() => yasqe.showNotification("autocomplete_property", "Failed fetching suggestions"))
  });

  Yasgui.Yasqe.forkAutocompleter("class", {
    name: "class-local",
    persistenceId: null,
    get: (yasqe, token) => window.info.then(data => data.classes.filter(iri => iri.startsWith(token.autocompletionString)))
      .catch(() => yasqe.showNotification("autocomplete_class", "Failed fetching suggestions"))
  });

  new Yasgui(document.getElementById('yasgui'), {
    endpointCatalogueOptions: {
      getData: () => [{
        endpoint: endpoint
      }]
    },
    yasqe : {
      value: "SELECT * WHERE\n{\n  ?S ?P ?O.\n}\nLIMIT 10"
    },
    requestConfig: {
      endpoint: endpoint
    },
    copyEndpointOnNewTab: false,
    persistenceId: null
  });

  window.history.replaceState({}, document.title, endpoint);
}
