# jDoubleAuctionMarket

## Características añadidas
* Ambiente híbrido que soporta CArtAgO y TimeSteppedEnviroment.
* Implementación de estrategia Moving Average.
* Implementación de agente con estrategia definida en Wolfram Language.
* Inicialización de parámetros por medio de artefacto CArtAgO con interfaz gráfica.
* Generación de Log de libro de órdenes.

## Descripción del experimento
Se realizo la ejecución de una simulación de una subasta doble que representa una bolsa de valores en la cual operan dos tipos de agentes:

* Agentes con Zero Inteligencia (ZI): Son agentes que realizan operaciones de compra/venta dependiendo de variables aleatorias generadas por distribuciones de probabilidad.
* Agentes Media Móvil (MA): Son agentes que deciden si comprar o vender dependiendo de la cruza de medias móviles.

![maPlot](https://raw.githubusercontent.com/CarlosManuelRodr/jDoubleAuctionMarket/master/img/stockMA.png)

Para el experimento se dejó una cantidad fija de 100 agentes, y se varió la proporción de agentes del tipo ZI y MA.

## Resultados
Al evaluar los resultados añadiendo mayor proporción de agentes que siguen la estrategia Moving Average (MA) se observa que el comportamiento de la serie de tiempo de precios adquiere periodicidad hasta llegar a un punto crítico entre 75% y 80% de agentes MA en donde la simulación converge a un precio estacionario.

![prices](https://raw.githubusercontent.com/CarlosManuelRodr/jDoubleAuctionMarket/master/img/price_series.jpeg)

Al comparar con las distribuciones de retornos generadas por la misma serie de precios se observa que al añadir mayor proporción de agentes MA las colas de las distribuciones de retornos se hacen cada vez más pesadas, lo cual es una propiedad deseable de una simulación de un mercado.

![prices](https://raw.githubusercontent.com/CarlosManuelRodr/jDoubleAuctionMarket/master/img/distributions.jpeg)

Comparando la riqueza promedio de los agentes tipo MA con los ZI es posible concluir que la estrategia MA genera una ventaja significativa con respecto a la estrategia ZI.

![comparison](https://raw.githubusercontent.com/CarlosManuelRodr/jDoubleAuctionMarket/master/img/wealth_comparison.jpeg)

![dist](https://raw.githubusercontent.com/CarlosManuelRodr/jDoubleAuctionMarket/master/img/output.gif)