import java.util.ArrayList;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import java.util.Comparator;
import java.util.Collections;
import javalib.worldimages.*;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Random;

//================================================================================
// SETUP
//================================================================================

//represents the user playing the game
class Player {
  Vertex v;
  
  Player(Vertex v) {
    this.v = v;
  }
  
  //draws the user
  WorldImage drawPlayer() {
    return new RectangleImage(MazeWorld.CELL_SIZE - 3, MazeWorld.CELL_SIZE - 3, 
        OutlineMode.SOLID, Color.CYAN).movePinhole(-MazeWorld.CELL_SIZE / 2, 
            -MazeWorld.CELL_SIZE / 2);
  }
  
  //determines whether or not the user is making a valid move
  boolean isValidMove(String move) {
    if (move.equals("left") && this.v.left != null) {
      return !this.v.left.renderedRight;
    }
    else if (move.equals("right") && this.v.right != null) {
      return !this.v.renderedRight;
    }
    else if (move.equals("up") && this.v.top != null) {
      return !this.v.top.renderedBottom;
    }
    else if (move.equals("down") && this.v.bottom != null) {
      return !this.v.renderedBottom;
    }
    else {
      return false;
    }
  }
}

//represents a vertex
class Vertex {
  ArrayList<Edge> loEdges = new ArrayList<Edge>();
  
  int x;
  int y;
  
  Vertex left;
  Vertex right;
  Vertex top;
  Vertex bottom;
  Vertex prev;
  
  boolean renderedRight;
  boolean renderedBottom;
  boolean hasTraveled;

  Vertex(int x, int y) {
    this.x = x;
    this.y = y;
    
    this.left = null;
    this.right = null;
    this.top = null;
    this.bottom = null;
    this.prev = null;
    
    this.renderedRight = true;
    this.renderedBottom = true;
    this.hasTraveled = false;
  }

  //draws a rectangle
  WorldImage draw(int x, int y, Color c) {
    return new RectangleImage(MazeWorld.CELL_SIZE - 2, MazeWorld.CELL_SIZE - 2, 
        OutlineMode.SOLID, c).movePinhole(-x * MazeWorld.CELL_SIZE / x / 2, 
            -x * MazeWorld.CELL_SIZE / x / 2);
  }
  
  //draws a wall to the right
  WorldImage drawRight() {
    return new LineImage(new Posn(0, MazeWorld.CELL_SIZE), Color.BLACK)
        .movePinhole(-1 * MazeWorld.CELL_SIZE, MazeWorld.CELL_SIZE / -2);
  }

  //draws a wall to the bottom
  WorldImage drawBottom() {
    return new LineImage(new Posn(MazeWorld.CELL_SIZE, 0), Color.BLACK)
        .movePinhole(MazeWorld.CELL_SIZE / -2, -1 * MazeWorld.CELL_SIZE);
  }
}

//represents an edge
class Edge {
  int weight;
  
  Vertex from;
  Vertex to;

  Edge(Vertex from, Vertex to, int weight) {
    this.weight = weight;
    
    this.from = from;
    this.to = to;
  }
}

//compares the weights of two edges
class WeightComparator implements Comparator<Edge> {

  //compares the weights of two edges
  public int compare(Edge i1, Edge i2) {
    return i1.weight - i2.weight;
  }
}

//================================================================================
// GRAPH ALGORITHMS
//================================================================================

//represents either a queue or a stack
interface ICollection<T> {
  //returns the size of this ICollection
  int getSize();
  
  //adds an element to this ICollection
  void add(T element);
  
  //removes an element from this ICollection
  T remove();
}

//represents a queue (for breadth-first search)
class Queue<T> implements ICollection<T> {
  Deque<T> d;
  
  Queue() {
    this.d = new ArrayDeque<T>();
  }

  public int getSize() {
    return this.d.size();
  }

  public void add(T element) {
    this.d.addLast(element);
  }

  public T remove() {
    return this.d.removeFirst();
  }
}

//represents a stack (for depth-first search)
class Stack<T> implements ICollection<T> {
  Deque<T> d;
  
  Stack() {
    this.d = new ArrayDeque<T>();
  }
  
  public int getSize() {
    return this.d.size();
  }
  
  public void add(T element) {
    this.d.addFirst(element);
  }
  
  public T remove() {
    return this.d.removeFirst();
  }
}

//represents both algorithms (BFS & DFS) used to solve maze
class GraphAlgorithms {
  ArrayList<Vertex> vertices;
  
  GraphAlgorithms() {}
  
  //implements breadth-first search (BFS)
  ArrayList<Vertex> bfSearch(Vertex from, Vertex to) {
    return this.constructPath(from, to, new Queue<Vertex>());
  }
  
  //implements depth-first search (DFS)
  ArrayList<Vertex> dfSearch(Vertex from, Vertex to) {
    return this.constructPath(from, to, new Stack<Vertex>());
  }
  
  //constructs winning path
  ArrayList<Vertex> constructPath(Vertex from, Vertex to, ICollection<Vertex> list) {
    ArrayList<Vertex> path = new ArrayList<Vertex>();
    list.add(from);
    
    while (list.getSize() > 0) {
      Vertex v = list.remove();
      
      if (v == to) {
        return path;
      }
      else if (!path.contains(v)) {
        for (Edge e : v.loEdges) {
          list.add(e.from);
          list.add(e.to);
          
          if (path.contains(e.from)) {
            v.prev = e.from;
          }
          else if (path.contains(e.to)) {
            v.prev = e.to;
          }
        }
        path.add(v);
      }
    }
    
    return path;
  }
}

//================================================================================
// GAME
//================================================================================

//represents game world
class MazeWorld extends World {
  WorldScene scene = new WorldScene(0, 0);
  
  ArrayList<ArrayList<Vertex>> board;
  ArrayList<Vertex> path = new ArrayList<Vertex>();
  ArrayList<Edge> loe = new ArrayList<Edge>();
  ArrayList<Edge> mst = new ArrayList<Edge>();
  HashMap<Vertex, Vertex> map = new HashMap<Vertex, Vertex>();
  
  static final int CELL_SIZE = 25;
  int sizeX;
  int sizeY;
  double time;
  double tickRate = 0.01;
  
  boolean finished;
  
  Player player;
  
  Vertex lastCell;
  
  TextImage timeLeft;

  MazeWorld(int sizeX, int sizeY) {
    this.sizeX = sizeX;
    this.sizeY = sizeY;
    this.board = this.buildGrid(sizeX, sizeY);
    this.finished = false;
    
    this.buildEdges(this.board);
    this.buildMap(board);
    this.kruskalsAlgorithm();
    
    this.time = this.sizeX * this.sizeY;
    this.timeLeft = new TextImage("TIME: " + (int) this.time, 15, Color.BLACK);
    this.player = new Player(board.get(0).get(0));
    
    this.lastCell = this.board.get(sizeY - 1).get(sizeX - 1);
    this.drawWorld();
  }

  //draws maze world grid as well as player and time
  WorldScene drawWorld() {
    this.scene.placeImageXY(board.get(0).get(0).draw(this.sizeX, this.sizeY, Color.GREEN),
        0, 0);
    this.scene.placeImageXY(
        board.get(this.sizeY - 1).get(this.sizeX - 1).draw(this.sizeX,
            this.sizeY, Color.RED),
        (sizeX - 1) * CELL_SIZE, (sizeY - 1) * CELL_SIZE);
    
    for (int x = 0; x < sizeY; x++) {
      for (int y = 0; y < sizeX; y++) {
        this.changeRenderedBottom(this.board.get(x).get(y));
        this.changeRenderedRight(this.board.get(x).get(y));
        
        if (board.get(x).get(y).renderedRight) {
          this.scene.placeImageXY(board.get(x).get(y).drawRight(), (MazeWorld.CELL_SIZE * y),
              (MazeWorld.CELL_SIZE * x));
        }
        
        if (board.get(x).get(y).renderedBottom) {
          this.scene.placeImageXY(board.get(x).get(y).drawBottom(), (MazeWorld.CELL_SIZE * y),
              (MazeWorld.CELL_SIZE * x));
        }
        
        if (board.get(x).get(y).hasTraveled) {
          this.scene.placeImageXY(board.get(x).get(y).draw(this.sizeX,  this.sizeY, Color.BLUE), 
              y * CELL_SIZE, x * CELL_SIZE);
        }
      }
    }
    
    this.scene.placeImageXY(player.drawPlayer(), this.player.v.x * CELL_SIZE, 
        this.player.v.y * CELL_SIZE);
    this.scene.placeImageXY(this.timeLeft, CELL_SIZE + 20, sizeY * CELL_SIZE + 
        CELL_SIZE / 2);
    
    return scene;
  }

  //updates maze world per tick
  public WorldScene makeScene() {
    
    if (path.size() > 1) {
      this.getEnd();
    }
    else if (path.size() > 0) {
      this.drawEnd();
    }
    else if (this.lastCell.prev != null && this.finished) {
      this.highlightSolution();
    }
    
    if (this.player.v != this.board.get(this.sizeY - 1).get(this.sizeX - 1) && 
        this.player.v != this.lastCell) {
      this.time = this.time - this.tickRate;
      this.timeLeft.text = "TIME: " + (int) this.time;
    }
    
    if (player.v == this.lastCell) {
      this.scene.placeImageXY(new TextImage("YOU WON :O", 25, Color.BLACK), sizeX * CELL_SIZE / 2, 
          sizeY * CELL_SIZE / 2);
    }
    
    if (this.time <= 0) {
      this.scene.placeImageXY(new TextImage("YOU LOSE :)", 25, Color.BLACK), sizeX * CELL_SIZE / 2, 
          sizeY * CELL_SIZE / 2);
      this.time = 0;
    }
    
    return scene;
  }

  //constructs maze world grid
  ArrayList<ArrayList<Vertex>> buildGrid(int bWidth, int bHeight) {
    ArrayList<ArrayList<Vertex>> board = new ArrayList<ArrayList<Vertex>>();
    
    for (int x = 0; x < bHeight; x++) {
      board.add(new ArrayList<Vertex>());
      ArrayList<Vertex> l = board.get(x);
      
      for (int y = 0; y < bWidth; y++) {
        l.add(new Vertex(y, x));
      }
    }
    
    this.linkVertices(board);
    this.buildEdges(board);
    this.buildMap(board);
    
    return board;
  }

  //constructs game world edges
  ArrayList<Edge> buildEdges(ArrayList<ArrayList<Vertex>> n) {
    Random rw = new Random();
    
    for (int x = 0; x < n.size(); x++) {
      for (int y = 0; y < n.get(x).size(); y++) {
        if (y < n.get(x).size() - 1) {
          loe.add(new Edge(n.get(x).get(y), n.get(x).get(y).right, rw.nextInt(50)));
        }
        if (x < n.size() - 1) {
          loe.add(
              new Edge(n.get(x).get(y), n.get(x).get(y).bottom, (int) rw.nextInt(50)));
        }
      }
    }
    
    Collections.sort(loe, new WeightComparator());
    
    return this.loe;
  }

  //constructs game world map
  HashMap<Vertex, Vertex> buildMap(ArrayList<ArrayList<Vertex>> vertex) {
    for (int x = 0; x < vertex.size(); x++) {
      for (int y = 0; y < vertex.get(x).size(); y++) {
        this.map.put(vertex.get(x).get(y), vertex.get(x).get(y));
      }
    }
    
    return map;
  }

  //implements kruskal's algorithm for mst
  ArrayList<Edge> kruskalsAlgorithm() {
    int n = 0;
    
    while (this.mst.size() < this.loe.size() && n < this.loe.size()) {
      Edge e = loe.get(n);
      
      if (!this.find(this.find(e.from)).equals(this.find(this.find(e.to)))) {
        mst.add(e);
        union(this.find(e.from), this.find(e.to));
      }
      
      n += 1;
    }
    
    for (int y = 0; y < this.sizeY; y += 1) {
      for (int x = 0; x < this.sizeX; x += 1) {
        for (Edge e : this.mst) {
          if (this.board.get(y).get(x).equals(e.from) || this.board.get(y).get(x).equals(e.to)) {
            this.board.get(y).get(x).loEdges.add(e);
          }
        }
      }
    }
    
    return this.mst;
  }

  //locates vertex for this node
  Vertex find(Vertex v) {
    if (v.equals(this.map.get(v))) {
      return v;
    }
    else {
      return this.find(this.map.get(v));
    }
  }
  
  //updates maze world according to key selected by player
  public void onKeyEvent(String k) {
    if (k.equals("left") && player.isValidMove("left")) {
      player.v.hasTraveled = true;
      player.v = player.v.left;
    }
    else if (k.equals("right") && player.isValidMove("right")) {
      player.v.hasTraveled = true;
      player.v = player.v.right;
    }
    else if (k.equals("up") && player.isValidMove("up")) {
      player.v.hasTraveled = true;
      player.v = player.v.top;
    }
    else if (k.equals("down") && player.isValidMove("down")) {
      player.v.hasTraveled = true;
      player.v = player.v.bottom;
    }
    else if (k.equals("b")) {
      this.lastCell = this.board.get(this.sizeY - 1).get(this.sizeX - 1);
      this.path = new GraphAlgorithms().bfSearch(this.board.get(0).get(0), 
          this.board.get(this.sizeY - 1).get(this.sizeX - 1));
    }
    else if (k.equals("d")) {
      this.lastCell = this.board.get(this.sizeY - 1).get(this.sizeX - 1);
      this.path = new GraphAlgorithms().dfSearch(this.board.get(0).get(0), 
          this.board.get(this.sizeY - 1).get(this.sizeX - 1));
    }
    else if (k.equals("r")) {
      this.scene = this.getEmptyScene();
      this.board = this.buildGrid(sizeX, sizeY);
      this.time = this.sizeX * this.sizeY + 1;
      this.player = new Player(board.get(0).get(0));
      
      this.buildEdges(this.board);
      this.buildMap(this.board);
      this.kruskalsAlgorithm();
      
      this.lastCell = this.board.get(this.sizeY - 1).get(this.sizeX - 1);
      this.drawWorld();
    }
    
    this.scene.placeImageXY(player.drawPlayer(), player.v.x * CELL_SIZE, 
        player.v.y * CELL_SIZE);
    this.drawWorld();
  }
  
  //highlights winning path
  void highlightSolution() {
    if (this.lastCell.x == this.sizeX - 1 && this.lastCell.y == this.sizeY - 1) {
      this.scene.placeImageXY(this.lastCell.draw(this.sizeX,  this.sizeY,  Color.MAGENTA), 
          this.lastCell.x * CELL_SIZE, this.lastCell.y * CELL_SIZE);
    }
    
    this.scene.placeImageXY(this.lastCell.prev.draw(this.sizeX, this.sizeY, 
        Color.MAGENTA), this.lastCell.prev.x * CELL_SIZE, this.lastCell.prev.y * 
        CELL_SIZE);
    
    this.lastCell = this.lastCell.prev;
  }
  
  //finds winning path to end of maze world
  void getEnd() {
    Vertex v = path.remove(0);
    this.scene.placeImageXY(v.draw(this.sizeX,  this.sizeY,  Color.YELLOW), 
        v.x * CELL_SIZE, v.y * CELL_SIZE);
  }
  
  //draws winning path to end of maze world
  void drawEnd() {
    Vertex v = path.remove(0);
    this.scene.placeImageXY(v.draw(this.sizeX,  this.sizeY,  Color.YELLOW), 
        v.x * CELL_SIZE, v.y * CELL_SIZE);
    
    if (this.lastCell.left.prev != null && !this.lastCell.left.renderedRight) {
      this.lastCell.prev = this.lastCell.left;
    }
    else if (this.lastCell.top.prev != null && !this.lastCell.top.renderedBottom) {
      this.lastCell.prev = this.lastCell.top;
    }
    else {
      this.lastCell.prev = v;
    }
    
    this.finished = true;
  }
  
  //connects vertices
  void linkVertices(ArrayList<ArrayList<Vertex>> l) {
    for (int x = 0; x < this.sizeY; x++) {
      for (int y = 0; y < this.sizeX; y++) {
        if (y + 1 < this.sizeX) {
          l.get(x).get(y).right = l.get(x).get(y + 1);
        }
        if (y - 1 >= 0) {
          l.get(x).get(y).left = l.get(x).get(y - 1);
        }
        if (x + 1 < this.sizeY) {
          l.get(x).get(y).bottom = l.get(x + 1).get(y);
        }
        if (x - 1 >= 0) {
          l.get(x).get(y).top = l.get(x - 1).get(y);
        }
      }
    }
  }
  
  //joins vertices
  void union(Vertex v1, Vertex v2) {
    this.map.put(this.find(v1), this.find(v2));
  }

  //determines whether or not right edge should be rendered
  void changeRenderedRight(Vertex v) {
    for (Edge edge : this.mst) {
      if (edge.to.y == edge.from.y) {
        edge.from.renderedRight = false;
      }
    }
  }

  //determines whether or not bottom edge should be rendered
  void changeRenderedBottom(Vertex v) {
    for (Edge edge : this.mst) {
      if (edge.to.x == edge.from.x) {
        edge.from.renderedBottom = false;
      }
    }
  }
}

//================================================================================
// EXAMPLES
//================================================================================

//examples & tests for game
class ExamplesTwistyMazeGame {
  MazeWorld buildMaze = new MazeWorld(25, 25);

  //tests big bang
  void testBigBang(Tester t) {
    this.buildMaze.bigBang(this.buildMaze.sizeX * MazeWorld.CELL_SIZE, 
        this.buildMaze.sizeY * MazeWorld.CELL_SIZE + MazeWorld.CELL_SIZE, this.buildMaze.tickRate);
  }
}