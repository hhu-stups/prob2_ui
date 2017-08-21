# A plugin mechanism for ProB2-UI

This fork adds a plugin mechanism to the ProB2-UI application. 
As framework we use the [Plugin Framework 4 Java](https://github.com/decebals/pf4j). 

The following sections describe how these plugins can be used in ProB2-UI and how you can develop your own plugins.

## Developing a plugin

There are three classes that you need to know about if you want to develop your own plugins. 
These classes are:
* <b>ProBPlugin:</b><p>
   Extends the plugn class of [PF4J](https://github.com/decebals/pf4j). For your own plugin you need a class which extends this one.
   In the `startPlugin()` method you should create your plugin and add all the UI-elements and listeners your plugin need. -
   In the `stopPlugin()` method it is necessary to remove all added listeners and UI-elements.
* <b>ProBPluginManager:</b><p>
   Extends the `JarPluginManager` class of [PF4J](https://github.com/decebals/pf4j). 
   The ProB2-UI application uses only a singleton instance of the ProBPluginManager.
   You can use this class to manage the plugins and extension points you need in your plugin.
* <b>ProBConnection:</b><p>
   This class is a connection between your plugin and the ProB2-UI application. It has some methods 
   to add elements to GUI (like tabs for ex.).
   
If you want to use other classes of the ProB2-UI application,
please use the Guice-injector. You can get an instance of the injector with the
`getInjector()`-method of the ProBPlugin class.  

To build your plugin, add the following code to your `buid.gradle` file. 
You have to replace the values in brackets with your own values. A example is shown in the 
[EventB-Pacman]() plugin repository. 
The `PROB_VERSION` value has to match the ProB2-UI version you are targeting.

```
jar {
    baseName = 'EventB-Pacman'
    version = '0.1.0'
    manifest {
        attributes 'Plugin-Class' : '[PLUGIN_CLASS]',
                'Plugin-Id' : '[PLUGIN_ID]',
                'Plugin-Version' : '[PLUGIN_VERSION]',
                'Plugin-Provider' : '[PLUGIN_PROVIDER]',
                'Plugin-Requires' : '[PROB_VERSION]'
    }
    from {
        configurations.compile.collect { it.isDirectory() ? it : zipTree(it) }
    }
}
```
## Using a plugin
//TODO

  

