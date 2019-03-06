# Description
This provides the `ConnGraph` class, which implements an undirected graph with
dynamic connectivity. It supports adding and removing edges and determining
whether two vertices are connected - whether there is a path between them.
Adding and removing edges take O(log<sup>2</sup>N) amortized time with high
probability, while checking whether two vertices are connected takes O(log N)
time with high probability. Check the source code to see the full API and the
Javadoc documentation.

# Features
* Efficiently add and remove edges and determine whether vertices are connected.
* A vertex can appear in multiple graphs, with a different set of adjacent
  vertices in each graph.
* `ConnGraph` supports arbitrary vertex augmentation. Given a vertex V,
  `ConnGraph` can quickly report the result of combining the augmentations of
  all of the vertices in the connected component containing V, using a combining
  function provided to the constructor. For example, if a `ConnGraph` represents
  a game map, then given the location of the player, we can quickly determine
  the amount of gold the player can access, or the strongest monster that can
  reach him. Retrieving the combined augmentation for a connected component
  takes O(log N) time with high probability.
* Compatible with Java 6.0 and above.

# Limitations
* `ConnGraph` does not directly support augmenting edges. However, this can be
  accomplished by imputing each edge's augmentation to an adjacent vertex. For
  example, if each edge contains a certain amount of gold, then we can augment
  each vertex with the amount of gold in the adjacent edges. We can then
  calculate the amount of gold in a connected component by retrieving the
  component's augmentation and dividing by two. A more general approach would be
  to store the edges adjacent to each vertex in an augmented self-balancing
  binary search tree (see
  [RedBlackNode](https://github.com/btrekkie/RedBlackNode)), and to use this to
  assign an augmentation to each vertex.
* Careful attention has been paid to the asymptotic running time of each method.
  However, beyond this, no special effort has been made to optimize performance.

# Example usage
```java
ConnGraph graph = new ConnGraph();
ConnVertex vertex1 = new ConnVertex();
ConnVertex vertex2 = new ConnVertex();
ConnVertex vertex3 = new ConnVertex();
graph.addEdge(vertex1, vertex2);
graph.addEdge(vertex2, vertex3);
graph.connected(vertex1, vertex3);  // Returns true
graph.removeEdge(vertex1, vertex2);
graph.connected(vertex1, vertex3);  // Returns false
```
