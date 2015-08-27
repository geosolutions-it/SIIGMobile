SIIG Mobile
===========

Mobile version of the [SIIG](http://www.csipiemonte.it/web/it/portfolio/ambiente/362-sistema-informativo-integrato-globale-destination)

This codebase build two different application flavours:
* SIIG Mobile - Elaborazione Standard
* SIIG Mobile - Valutazione del Danno

NewRelic Setup
--------------

In order to set the correct NewRelic Application Token for each flavor, fill the following properties in the gradle.properties file in your build server home directory

```
elaborazionestandardNewRelicApplicationToken=
valutazionedannoNewRelicApplicationToken=
```