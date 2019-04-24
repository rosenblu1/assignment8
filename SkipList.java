import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;
import java.util.function.BiConsumer;

/**
 * An implementation of skip lists.
 */
public class SkipList<K, V> implements SimpleMap<K, V> {

  // +-----------+---------------------------------------------------
  // | Constants |
  // +-----------+

  /**
   * The initial height of the skip list.
   */
  static final int INITIAL_HEIGHT = 16;

  // +---------------+-----------------------------------------------
  // | Static Fields |
  // +---------------+

  static Random rand = new Random();

  // +--------+------------------------------------------------------
  // | Fields |
  // +--------+

  /**
   * Pointers to all the front elements.
   */
  ArrayList<SLNode<K, V>> front;

  /**
   * The comparator used to determine the ordering in the list.
   */
  Comparator<K> comparator;

  /**
   * The number of values in the list.
   */
  int size;

  /**
   * The current height of the skiplist.
   */
  int height;

  /**
   * The probability used to determine the height of nodes.
   */
  double prob = 0.5;

  // +--------------+------------------------------------------------
  // | Constructors |
  // +--------------+

  /**
   * Create a new skip list that orders values using the specified comparator.
   */
  public SkipList(Comparator<K> comparator) {
    this.front = new ArrayList<SLNode<K, V>>(INITIAL_HEIGHT);
    for (int i = 0; i < INITIAL_HEIGHT; i++) {
      front.add(null);
    } // for
    this.comparator = comparator;
    this.size = 0;
    this.height = INITIAL_HEIGHT;
  } // SkipList(Comparator<K>)

  /**
   * Create a new skip list that orders values using a not-very-clever default comparator.
   */
  public SkipList() {
    this((k1, k2) -> k1.toString().compareTo(k2.toString()));
  } // SkipList()


  // +-------------------+-------------------------------------------
  // | SimpleMap methods |
  // +-------------------+
  /**
   * Set the value associated with key.
   * 
   * @return the previous value associated with key (or null, if there's no such value)
   * 
   * @throws NullPointerException if the key is null.
   */

  @Override
  public V set(K key, V value) {

    if (key == null) {
      throw new NullPointerException("null key");
    } // if

    SLNode<K, V> node = front.get(front.size() - 1);

    // make newnode
    int h = randomHeight();
     System.out.println("The height is " + h);
    SLNode<K, V> newnode = new SLNode<K, V>(key, value, h);

    // if the list is empty
    if (this.size == 0) {
      if (h > this.height) {
        // update front
        for (int j = 0; j < h; j++) {
          front.add(newnode);
        }
        this.height = h;
      } // if h > this.height

      for (int i = 0; i < h; i++) {
        front.set(i, newnode); // make front point to newnode for h levels
        newnode.next.add(null); // fill 'next' field of newnode
      }

      this.size++;
      return value;
    } // if the list is empty
    else {

      node = search(key);

      // if key is already present
      if (node.key == key) {
        node.value = value;
        return value;
      } else {

        if (h > this.height) {
          // update front
          for (int j = 0; j < h - this.height; j++) {
            front.add(newnode);
          }
          this.height = h;
        } // if (h > this.height)

        ArrayList<SLNode<K, V>> nodes = this.getNodes(key);

        int topLevel = this.topLevel();

        for (int index = 0; index < nodes.size(); index++) {
          int lev = topLevel - index;
          if (h > lev) {
            newnode.setNext(lev, nodes.get(index).next(lev));// make newnode point to next element
            nodes.get(index).setNext(lev, newnode);// correct pointers to newnode
          }
        } // for

        // fix lowest level
        newnode.setNext(0, nodes.get(nodes.size() - 1).next(0));
        nodes.get(nodes.size() - 1).next(0).setNext(0, newnode);
      } // if we have to make and insert a newnode
      this.size++;
      return value;
    } // if list is not empty
  } // set(K,V)

  /**
   * Get the value associated with key.
   * 
   * @throws IndexOutOfBoundsException if the key is not in the map.
   * @throws NullPointerException if the key is null.
   */
  @Override
  public V get(K key) {

    if (key == null) {
      throw new NullPointerException("key is null");
    } else if (size == 0) {
      throw new IndexOutOfBoundsException("list is empty");
    }

    SLNode<K, V> node = search(key);

    if (comparator.compare(node.key, key) == 0) {
      return node.value;
    } else {
      throw new IndexOutOfBoundsException("key not found");
    }
  } // get(K,V)

  @Override
  public int size() {
    return this.size;
  } // size()

  @Override
  public boolean containsKey(K key) {
    if (key == null) {
      throw new NullPointerException("null key");
    } else if (this.size == 0) {
      return false;
    }

    SLNode<K, V> node = search(key);
    if (comparator.compare(node.key, key) == 0) {
      return true;
    } else {
      return false;
    }
  } // containsKey(K)

  @Override
  public V remove(K key) {
    // TODO Auto-generated method stub
    return null;
  } // remove(K)

  @Override
  public Iterator<K> keys() {
    return new Iterator<K>() {
      Iterator<SLNode<K, V>> nit = SkipList.this.nodes();

      @Override
      public boolean hasNext() {
        return nit.hasNext();
      } // hasNext()

      @Override
      public K next() {
        return nit.next().key;
      } // next()

      @Override
      public void remove() {
        nit.remove();
      } // remove()
    };
  } // keys()

  @Override
  public Iterator<V> values() {
    return new Iterator<V>() {
      Iterator<SLNode<K, V>> nit = SkipList.this.nodes();

      @Override
      public boolean hasNext() {
        return nit.hasNext();
      } // hasNext()

      @Override
      public V next() {
        return nit.next().value;
      } // next()

      @Override
      public void remove() {
        nit.remove();
      } // remove()
    };
  } // values()

  @Override
  public void forEach(BiConsumer<? super K, ? super V> action) {
    // TODO Auto-generated method stub

  } // forEach

  // +----------------------+----------------------------------------
  // | Other public methods |
  // +----------------------+
  /**
   * Dump the list to some output location.
   */
  public void dump(PrintWriter pen) {
    String leading = "          ";

    SLNode<K, V> current = front.get(0);

    // Print some X's at the start
    pen.print(leading);
    for (int level = 0; level < this.height; level++) {
      pen.print(" X");
    } // for
    pen.println();
    // printLinks(pen, leading);

    while (current != null) {
      // Print out the key as a fixed-width field.
      // (There's probably a better way to do this.)
      String str;
      if (current.key == null) {
        str = "<null>";
      } else {
        str = current.key.toString();
      } // if/else
      if (str.length() < leading.length()) {
        pen.print(leading.substring(str.length()) + str);
      } else {
        pen.print(str.substring(0, leading.length()));
      } // if/else

      // Print an indication for the links it has.
      for (int level = 0; level < current.next.size(); level++) {
        pen.print("-*");
      } // for
      // Print an indication for the links it lacks.
      for (int level = current.next.size(); level < this.height; level++) {
        pen.print(" |");
      } // for
      pen.println();
      // printLinks(pen, leading);

      current = current.next.get(0);
    } // while

    // Print some O's at the start
    pen.print(leading);
    for (int level = 0; level < this.height; level++) {
      pen.print(" O");
    } // for
    pen.println();

  } // dump(PrintWriter)

  /**
   * Print some links (for dump).
   */
  void printLinks(PrintWriter pen, String leading) {
    pen.print(leading);
    for (int level = 0; level < this.height; level++) {
      pen.print(" |");
    } // for
    pen.println();
  } // printLinks
  // +---------+-----------------------------------------------------
  // | Helpers |
  // +---------+

  /**
   * Pick a random height for a new node.
   */
  int randomHeight() {
    int result = 1;
    while (rand.nextDouble() < prob) {
      result = result + 1;
    }
    return result;
  } // randomHeight()

  int topLevel() {
    int lev = this.height - 1;
    SLNode<K, V> node = front.get(lev);

    // get down to the first actual value of front
    while (node == null) {
      node = front.get(lev);
      lev--;
    }
    return lev + 1;
  }

  ArrayList<SLNode<K, V>> getNodes(K key) {
    ArrayList<SLNode<K, V>> path = new ArrayList<SLNode<K, V>>(1);

    int lev = this.topLevel();
    SLNode<K, V> node = front.get(lev);

    // large 'for' loop that moves down the levels searching (vertical)
    for (int level = lev; level >= 0; level--) {
      // invariant: node.key <= key (horizontal)
      while (comparator.compare(node.key, key) < 0 && node.next.get(level) != null) {
        node = node.next.get(level);
      } // while
      path.add(node);
    } // for
    return path;
  } // getNodes(K)

  /**
   * returns the node right in front of where searchKey is
   */
  SLNode<K, V> search(K key) {

    int lev = this.topLevel();
    SLNode<K, V> node = front.get(lev);

    // large 'for' loop that moves down the levels searching (vertical)
    for (int level = lev; level >= 0; level--) {
      // invariant: node.key <= key (horizontal)
      while (comparator.compare(node.key, key) < 0 && node.next.get(level) != null) {
        node = node.next.get(level);
      } // while
    } // for
    return node;
  } // search

  /**
   * Get an iterator for all of the nodes. (Useful for implementing the other iterators.)
   */
  Iterator<SLNode<K, V>> nodes() {
    return new Iterator<SLNode<K, V>>() {

      /**
       * A reference to the next node to return.
       */
      SLNode<K, V> next = SkipList.this.front.get(0);

      @Override
      public boolean hasNext() {
        return this.next != null;
      } // hasNext()

      @Override
      public SLNode<K, V> next() {
        if (this.next == null) {
          throw new IllegalStateException();
        }
        SLNode<K, V> temp = this.next;
        this.next = this.next.next.get(0);
        System.out.println("getting here");
        return temp;
      } // next();
    }; // new Iterator
  } // nodes()

  // +---------+-----------------------------------------------------
  // | Helpers |
  // +---------+

} // class SkipList


/**
 * Nodes in the skip list.
 */
class SLNode<K, V> {

  // +--------+------------------------------------------------------
  // | Fields |
  // +--------+

  /**
   * The key.
   */
  K key;

  /**
   * The value.
   */
  V value;

  /**
   * Pointers to the next nodes.
   */
  ArrayList<SLNode<K, V>> next;

  // +--------------+------------------------------------------------
  // | Constructors |
  // +--------------+

  /**
   * Create a new node of height n with the specified key and value.
   */
  public SLNode(K key, V value, int n) {
    this.key = key;
    this.value = value;
    this.next = new ArrayList<SLNode<K, V>>(n);
    for (int i = 0; i < n; i++) {
      this.next.add(null);
    } // for
  } // SLNode(K, V, int)

  // +---------+-----------------------------------------------------
  // | Methods |
  // +---------+
  /**
   * Get the next node at the specified level.
   */
  public SLNode<K, V> next(int level) {
    return this.next.get(level);
  } // next

  /**
   * Set the next node at the specified level.
   */
  public void setNext(int level, SLNode<K, V> next) {
    this.next.set(level, next);
  } // setNext(int, SLNode<K,V>)


} // SLNode<K,V>
