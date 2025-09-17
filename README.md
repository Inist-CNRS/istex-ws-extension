# ISTEX TDM Web Services OpenRefine extension

OpenRefine extension to apply [ISTEX TDM Web
Services](https://services.istex.fr/) on a column of text.  
It will generate a new column with the result of the query.  

## Development

### Principles

This extension, inspired from
[llm-extension](https://github.com/sunilnatraj/llm-extension), aims to allow
querying [ISTEX Web Services](https://openapi.services.istex.fr/) from
OpenRefine.

ISTEX Web Services are a set of APIs that allow to query the ISTEX corpus of
scholarly documents. They are documented at
<https://openapi.services.istex.fr/>.

These web services are thought to have the less parameters possible.  
Each route points to a different Text Mining algorithm (and its parameters).  
The body of most of the queries is a JSON object containing the text to be
processed.  
The response is a JSON object containing the results of the query.  

The query's structure is the following:

```json
[
    {
        "id": "optional unique id",
        "value": "text to be analyzed"
    },
    {
        "id": "another unique id",
        "value": "another text to be analyzed"
    }
]
```

The response's structure is the following:

```json
[
    {
        "id": "optional unique id",
        "value": {
            "result": "result of the algorithm"
        }
    },
    {
        "id": "another unique id",
        "value": {
            "result": "result of the algorithm"
        }
    }
]
```

### Example

The extension allows to query the
[affiliation country](https://openapi.services.istex.fr/#/address/post-v1-affiliationcountry)
service.  
This service takes an affiliation address and returns the country found in it.  
The query is the following:

```json
[
  {
    "id": 1,
    "value": "université sciences et technologies bordeaux 1 institut national de physique nucléaire et de physique des particules du cnrs in2p3 UMR5797"
  },
  {
    "id": 2,
    "value": "uar76 / ups76 centre national de la recherche scientifique cnrs institut de l'information scientifique et technique inist"
  }
]
```

The response is the following:

```json
[
  {
    "id": 1,
    "value": {
      "country": "France",
      "code": "fr"
    }
  },
  {
    "id": 2,
    "value": {
      "country": "France",
      "code": "fr"
    }
  }
]
```

The point of the extension is to allow to query the web services from OpenRefine,
and to process the response in order to extract the information we want.  
In the example above, the user would choose to create a new column with the
country found in the affiliation address (`country` and `code` fields, as a JSON
object).

### Compile

To compile and install the extension, run the following command from the root of the project:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
mvn clean install
```

> [!NOTE]
> The `maven` and `openjdk-17-jdk` apt packages must be installed.

### Configuration

To see that extension within OpenRefine, you need to add the following line to the `webapp/WEB-INF/butterfly.properties` (in OpenRefine's directory) file:

```ini
butterfly.modules.path = ../../istex-ws-extension
```

### References

- Documentation: <https://openrefine.org/docs/technical-reference/writing-extensions>
- Example extension: <https://github.com/sunilnatraj/llm-extension>

## Usage

To use this extension, click on a column menu and select "TDM Enrichment".  
Then, type the name of the column you want to create, and then the URL of the service you want to use.  
Next, click on OK.

See [ISTEX TDM](https://services.istex.fr) for more information about the available services.  

> [!TIP]
> Only use non-asynchronous services (services working on a single text).

## Install this extension in OpenRefine

Download the .zip file of the [latest release of this extension](https://github.com/Inist-CNRS/istex-ws-extension/releases).  
Unzip this file and place the unzipped folder in your OpenRefine extensions folder. [Read more about installing extensions in OpenRefine's user manual](https://docs.openrefine.org/manual/installing#installing-extensions).  

When this extension is installed correctly, when you open a project you will see in the Extensions bar TDM menu.
