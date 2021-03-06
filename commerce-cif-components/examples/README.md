# AEM CIF Core Components Library

This folder contains the projects required to build the CIF Components Library that extends the [WCM Core Components Library](https://www.aemcomponents.dev/) with the commerce components.

## Prerequisites

The CIF Components Library must be installed on top of the WCM Core Components Library, so **make sure** you first install both the latest [ui.content](https://repo1.maven.org/maven2/com/adobe/cq/core.wcm.components.examples.ui.content/2.9.0/core.wcm.components.examples.ui.content-2.9.0.zip) and [ui.apps](https://repo1.maven.org/maven2/com/adobe/cq/core.wcm.components.examples.ui.apps/2.9.0/core.wcm.components.examples.ui.apps-2.9.0.zip) content packages of the WCM Core Components Library.

You will also need the latest version of the WCM Core Components, the easiest is to install the ["all" content package](https://repo1.maven.org/maven2/com/adobe/cq/core.wcm.components.all/2.9.0/core.wcm.components.all-2.9.0.zip).

Simply download the zip files, and install these three content packages directly in AEM's CRX Package Manager.

## Installation

There are three sub-projects for the CIF Core Components Library:
* **bundle**: this contains a mock GraphQL server that will serve mock responses to all the example components.
* **ui.apps**: this contains some application content for the library, including the OSGi configuration of the services required by the examples.
* **ui.content**: this contains the example pages demonstrating the use of the CIF Components.

You can install all 3 artifacts by running `mvn clean install -PautoInstallPackage`

_Note that the `ui.apps` examples content package depends on the same version of the `ui.apps` content package of the CIF components. This means that a developer working on the SNAPSHOT version of the library must ensure that the same SNAPSHOT version of the components `ui.apps` library is installed on AEM._

## Installation with 'examples-all' package

This folder also contains an `all` content package that can be used to deploy the CIF components library and most of its dependencies:
* the CIF Components and bundle
* the GraphQL client bundle
* the Magento GraphQL bundle
* the WCM Core Components library when building with the Maven `-Pinclude-wcm-components-examples` profile

Note that the WCM Core components are not included: they are installed by default in the AEM Cloud SDK, and should be installed separately on a classic AEM instance.

To build and install that content package in a running AEM instance, simply use `mvn clean install content-package:install`.

## Required configuration

Starting with version `1.6.1`, the GraphQL client does no longer enforce HTTPS: the support for HTTP can be enabled in the client OSGi configuration, but this should NOT be done on production systems!

If you still want to use HTTPS, simply follow [this documentation](https://docs.adobe.com/content/help/en/experience-manager-65/administering/security/ssl-by-default.html) to enable HTTPS in your AEM instance.
If the self-signed certificate gets rejected by the browser, try adding it to the OS keychain and mark it as trusted.

On an author instance you must also enable anonymous access to the mock GraphQL server which will by defaut receive its requests on `/apps/cif-components-examples/graphql`. To do that, do the following:
* In the AEM system configuration console, look for `Apache Sling Authentication Service`
* Add the following line at the bottom of the "Authentication Requirements" property: `-/apps/cif-components-examples/graphql`

The GraphQL client used by the examples is configured by default with the URL set to `$[env:CIF_MOCK_GRAPHQL_ENDPOINT]`, which means that it would use the environment variable `CIF_MOCK_GRAPHQL_ENDPOINT` (for local development, this is only supported by the Cloud SDK). For local development, if you do not want or cannot set that variable, just override that OSGi configuration manually and set it to `https://localhost:8443/apps/cif-components-examples/graphql` for HTTPS or `http://localhost:4502/apps/cif-components-examples/graphql` for HTTP on an author instance with default port 4502.

## How does it work?

When everything is installed, you should find the `com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl~examples` and `com.adobe.cq.commerce.graphql.magento.GraphqlDataServiceImpl~examples` configurations in the AEM configuration console. These configure the GraphQL client and data service to point to the mock GraphQL server.

The components are configured to use the CIF configuration defined at `/conf/core-components-examples/settings/cloudconfigs/commerce`. This is configured via the usual `cq:conf` property defined at `/content/core-components-examples/library/commerce/jcr:content`.

All the CIF components used on the CIF library pages issue GraphQL requests to the mock GraphQL server which responds with mocked JSON responses that contain the data and links to images to be rendered by the component. We use a mock GraphQL server to avoid having any dependency on a pre-installed Magento instance with sample data.

When everything is correctly installed, you should be able to open the library page at [http://localhost:4502/content/core-components-examples/library.html](http://localhost:4502/content/core-components-examples/library.html) and see the "Commerce" section at the bottom of the left-side panel and at the bottom of the page content.

## Layout / design

**Note**: This is only useful for the developers of this components library.

The layout/design of the examples is currently "borrowed" from the [Venia theme](https://github.com/adobe/aem-cif-project-archetype/tree/master/src/main/archetype/ui.apps/src/main/content/jcr_root/apps/__appsFolderName__/clientlibs/theme) available in the CIF archetype. To avoid having any project dependency on the venia sample data, we generate the [venia.css](ui.apps/src/main/content/jcr_root/apps/cif-components-examples/clientlibs/venia-theme/venia.css) file "offline", based on the css files of the archetype sample data. This is done in 3 steps:

The `css.txt` file of the Venia theme is converted into a (css) `less` master file:

`sed "s/^#.*//;/^$/d;s/^.*$/@import (less) \"&\";/" css.txt`

The output of that command is copied into the placeholder in [venia.less.template](ui.apps/src/main/content/jcr_root/apps/cif-components-examples/clientlibs/venia-theme/venia.less.template) that you can save into a file called `venia.less`.

With the less compiler (install it with `npm install -g less`), execute the following command:

`lessc --verbose --math=strict venia.less venia.css`

This generates the `venia.css` file that we use for the layout/design of the examples.
