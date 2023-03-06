package test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class EnchantmentOptimizer {

	JFrame frame = new JFrame();
	List<JCheckBox> options = new ArrayList<>();
	List<JComboBox<Integer>> levels = new ArrayList<>();
	TreePanel treePanel = new TreePanel(new ArrayList<>());
	List<ETree> trees = new ArrayList<>();
	List<Enchantment> ench = new ArrayList<>();
	JCheckBox useXp = new JCheckBox("Use XP instead of levels.");

	public static void main(String[] args) {
		new EnchantmentOptimizer();
	}

	public EnchantmentOptimizer() {
		frame.setSize(1000, 500);
		frame.setLayout(new BorderLayout());

		JPanel sidebar = new JPanel();
		sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
		JScrollPane sideScroll = new JScrollPane(sidebar);
		sideScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		frame.add(sideScroll, BorderLayout.WEST);

		for (int i = 0; i < EnchantmentData.names.length; i++) {
			options.add(new JCheckBox(EnchantmentData.names[i]));
			Integer[] levelOptions = new Integer[EnchantmentData.maxLevel[i]];
			for (int j = 0; j < levelOptions.length; j++) {
				levelOptions[j] = j + 1;
			}
			levels.add(new JComboBox<>(levelOptions));
			JPanel row = new JPanel();
			row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));

			row.add(options.get(options.size() - 1));
			row.add(levels.get(levels.size() - 1));
			levels.get(levels.size() - 1).setSelectedItem(EnchantmentData.maxLevel[i]);
			levels.get(levels.size() - 1).addActionListener(this::settingsChanged);
			options.get(options.size() - 1).addActionListener(this::settingsChanged);
			sidebar.add(row);
		}
		sidebar.add(useXp);
		useXp.addActionListener(this::settingsChanged);

		frame.add(treePanel);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		settingsChanged(null);
	}

	public void settingsChanged(ActionEvent e) {
		List<Enchantment> en = new ArrayList<>();
		for (int i = 0; i < options.size(); i++) {
			if (options.get(i).isSelected()) {
				en.add(new Enchantment(options.get(i).getText(), (levels.get(i).getSelectedIndex() + 1) * EnchantmentData.multiplier[i]));
				if (useXp.isSelected()) {
					en.get(en.size() - 1).useXp();
				}
			}
		}
		List<ETree> trees = getBestTrees(en);
		if (useXp.isSelected()) {
			TreeOrder o = getBestTree(trees, en);

			en = o.ench();

			trees.clear();
			trees.add(o.tree());
		}
		ench = en;

		treePanel.trees = trees;
		treePanel.repaint();
		//		List<Enchantment> en2 = new ArrayList<>(en);
		//		en2.add(0, new Enchantment("Item", 0).item());
		//		forceOptimize(en2).forEach(System.out::println);

	}

	class TreePanel extends JPanel implements MouseMotionListener, MouseListener {

		List<ETree> trees;
		Map<Point, ETree> points = new HashMap<>();
		Map<Point, Enchantment> enchantmentPoints = new HashMap<>();
		Point hoverPoint;
		ETree hoverOver;
		Enchantment clicked;
		JComboBox<String> option = new JComboBox<String>(new String[] { "A", "B" });
		Map<Enchantment, Integer> enchantmentCopies = new HashMap<>();

		public TreePanel(List<ETree> trees) {
			this.trees = trees;
			addMouseMotionListener(this);
			addMouseListener(this);
			this.setLayout(null);
			this.add(option);
			option.setSize(200, 30);
			option.setLocation(-1000, 0);
			option.addActionListener(this::optionChanged);
		}

		public synchronized void optionChanged(ActionEvent e) {
			if (clicked != null) {
				System.out.println(option.getSelectedItem());
				for (Enchantment en : ench) {
					if (en.name.equals(option.getSelectedItem())) {
						option.setLocation(-1000, 0);
						int a = ench.indexOf(en);
						int b = ench.indexOf(clicked);
						System.out.println(en + ", " + clicked + " " + a + " " + b);
						ench.set(a, clicked);
						ench.set(b, en);
						clicked = null;
						repaint();
						return;
					}
				}
			}
		}

		public void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(Color.white);
			g2.fillRect(0, 0, getWidth(), getHeight());

			points.clear();
			enchantmentPoints.clear();
			enchantmentCopies.clear();
			for (int i = 0; i < trees.size(); i++) {
				ETree tree = trees.get(i);
				tree.invalidate();
				fill(tree, ench, false);
				tree.getCompleteEnchantment();
				g2.setColor(Color.black);

				drawTree(tree, tree, getWidth() / 2, getHeight() / trees.size() * i, getWidth(), g2);
			}
			if (hoverPoint != null) {
				String hover = "Select this tree.";
				g.drawString(hover, hoverPoint.x, hoverPoint.y);
			}
			if (trees.size() == 1) {
				List<ETree> steps = trees.get(0).getNodes(new ArrayList<>());
				FontMetrics fm = g2.getFontMetrics();
				int y = getHeight() / 2;
				g2.drawString("Steps: ", 10, y);

				g2.setColor(Color.black);
				for (ETree tree : steps) {
					if (tree.size() != 1) {
						y += fm.getHeight();

						y += fm.getHeight();
						g2.drawString("Combine " + tree.left.e + " and " + tree.right.e + "", 10, y);

						y += fm.getHeight();
						g2.drawString("to make " + tree.e + "", 10, y);
					}
				}
			}

		}

		public void drawTree(ETree root, ETree tree, int x, int y, int width, Graphics2D g2) {
			points.put(new Point(x, y + 10), root);
			enchantmentPoints.put(new Point(x, y + 10), tree.e);
			if (tree.e.isItem) {
				g2.setColor(new Color(200, 255, 255));
			} else if (tree.e.tax == 0) {
				g2.setColor(new Color(255, 200, 255));
			} else {
				g2.setColor(Color.white);
			}
			enchantmentCopies.put(tree.e, tree.multiplier);
			g2.fillOval(x - 10, y, 20, 20);
			g2.setColor(Color.black);
			FontMetrics fm = g2.getFontMetrics();
			//			String label = tree.multiplier + "";
			//			g2.drawString(label, x - fm.stringWidth(label) / 2, y + 10 + fm.getAscent() / 2);

			String text = tree.e.toString();
			//			String text = "(" + tree.e.cost + ", " + tree.e.tax + ", " + tree.e.weight + ")";
			g2.drawString(text, x - fm.stringWidth(text) / 2, y + 30);

			g2.drawOval(x - 10, y, 20, 20);

			if (tree.left != null) {
				g2.drawLine(x, y + 10, x - width / 4, y + 40 + 10);
				g2.drawLine(x, y + 10, x + width / 4, y + 40 + 10);
				drawTree(root, tree.left, x - width / 4, y + 40, width / 2, g2);
				drawTree(root, tree.right, x + width / 4, y + 40, width / 2, g2);
			}
		}

		public void mouseMoved(MouseEvent e) {
			hoverPoint = null;
			hoverOver = null;
			if (trees.size() > 1) for (Entry<Point, ETree> en : points.entrySet()) {
				if (en.getKey().distance(e.getX(), e.getY()) <= 20) {
					hoverPoint = new Point(e.getX(), e.getY());
					hoverOver = en.getValue();
				}
			}
			if (!option.getBounds().contains(e.getX(), e.getY())) {
				option.setLocation(-1000, e.getY());
			}
			repaint();
		}

		public void mousePressed(MouseEvent e) {
			if (trees.size() > 1) {
				for (Entry<Point, ETree> en : points.entrySet()) {
					if (en.getKey().distance(e.getX(), e.getY()) <= 20) {
						trees.clear();
						trees.add(en.getValue());
					}
				}
			} else {
				clicked = null;
				option.removeAllItems();
				for (Entry<Point, Enchantment> en : enchantmentPoints.entrySet()) {
					if (en.getKey().distance(e.getX(), e.getY()) <= 20) {
						option.setLocation(e.getX() - 10, e.getY() - 10);
						int target = getWeight(trees.get(0), ench);
						int pos = 0;
						Enchantment origin = null;
						for (int i = 0; i < ench.size(); i++) {
							if (en.getValue().equals(ench.get(i))) {
								pos = i;
								origin = ench.get(i);
							}
						}
						for (int i = 0; i < ench.size(); i++) {
							List<Enchantment> test = new ArrayList<>(ench);
							test.set(pos, test.get(i));
							test.set(i, origin);
							if (getWeight(trees.get(0), test) == target) {
								option.addItem(ench.get(i).name);
							}
						}
						option.setSelectedItem(origin.name);
						if (option.getItemCount() == 0) {
							option.setLocation(-1000, e.getY());
						}
						clicked = origin;
					}
				}
			}

			repaint();
		}

		public void mouseReleased(MouseEvent e) {
			if (!option.getBounds().contains(e.getX(), e.getY())) {
				option.setLocation(-1000, e.getY());
			}
		}

		public void mouseDragged(MouseEvent e) {}

		public void mouseClicked(MouseEvent e) {}

		public void mouseEntered(MouseEvent e) {}

		public void mouseExited(MouseEvent e) {}

	}

	public static List<ETree> getBestTrees(List<Enchantment> en) {
		List<ETree> trees = getAllViableTrees(en.size() * 2 + 1);

		List<ETree> best = new ArrayList<>();
		int bestV = Integer.MAX_VALUE;
		for (ETree t : trees) {
			fill(t, en, true);
			Enchantment e = t.getCompleteEnchantment();
			if (e.weight < 0) {
				continue;
			}
			if (e.weight < bestV) {
				best.clear();
				bestV = e.weight;
			}
			if (e.weight == bestV) {
				best.add(t);
			}

		}
		return best;
	}

	public static TreeOrder getBestTree(List<ETree> trees, List<Enchantment> en) {
		int best = Integer.MAX_VALUE;
		TreeOrder result = null;

		for (ETree tree : trees) {
			List<Enchantment> bestEn = findPermute(tree, en);
			if (getWeight(tree, bestEn) < best) {
				result = new TreeOrder(tree, bestEn);
				best = tree.e.weight;
			}

		}
		return result;
	}

	public static List<Enchantment> testPermute(ETree tree, List<Enchantment> en) {
		List<Enchantment> en2 = new ArrayList<>(en);
		en2.add(0, new Enchantment("Item", 0).item());
		List<ETree> nodes = tree.getLeaves(new ArrayList<>());
		nodes.sort(Comparator.comparingInt(n -> n.multiplier));

		List<Enchantment> best = new ArrayList<>();
		List<List<Enchantment>> allPermutes = findPermutations(en2, nodes);
		int bestV = Integer.MAX_VALUE;
		for (int i = 0; i < allPermutes.size(); i++) {
			allPermutes.get(i).remove(allPermutes.get(i).size() - 1);
			Collections.reverse(allPermutes.get(i));
			fill(tree, allPermutes.get(i), false);
			Enchantment e = tree.getCompleteEnchantment();
			if (e.weight < bestV) {
				best = allPermutes.get(i);
				bestV = e.weight;
			}
		}

		return best;
	}

	public static List<Enchantment> findPermute(ETree tree, List<Enchantment> en) {
		List<Enchantment> en2 = new ArrayList<>(en);
		boolean loop = true;
		int best = getWeight(tree, en2);
		loop: while (loop) {
			loop = false;
			for (int i = 0; i < en2.size(); i++) {
				for (int j = i + 1; j < en2.size(); j++) {
					swap(i, j, en2);
					if (getWeight(tree, en2) < best) {
						best = tree.e.weight;
						System.out.println(i + " " + j);
						loop = true;
						continue loop;
					}
					swap(i, j, en2);
				}
			}
		}
		return en2;
	}

	public static void swap(int a, int b, List<Enchantment> en) {
		Enchantment tmp = en.get(a);
		en.set(a, en.get(b));
		en.set(b, tmp);
	}

	public static int getWeight(ETree tree, List<Enchantment> en) {
		fill(tree, en, false);
		Enchantment e = tree.getCompleteEnchantment();
		return e.weight;
	}

	public static Enchantment approximate(List<Enchantment> en) {
		int[] values = new int[en.size() * 2 + 1];
		Enchantment[] enchantments = new Enchantment[en.size() * 2 + 1];

		List<Location> locations = fill(values, 0, 0);
		locations.sort(Comparator.comparingInt(l -> l.value * en.size() * 3 + l.position));
		en.sort(Comparator.comparingInt(e -> -e.cost));

		enchantments[locations.get(0).position] = new Enchantment("Item", 0).item();
		Iterator<Enchantment> eit = en.iterator();

		for (int i = 1; i < en.size() + 1; i++) {
			enchantments[locations.get(i).position] = eit.next();
		}
		fill(enchantments, 0);
		return enchantments[0];
	}

	public static Enchantment fill(ETree t, List<Enchantment> en, boolean sort) {
		if (sort) en.sort(Comparator.comparingInt(e -> -e.cost));
		t.invalidate();
		t.getMultiplier();
		List<ETree> nodes = t.getLeaves(new ArrayList<>());
		nodes.sort(Comparator.comparingInt(e -> e.multiplier));
		for (int i = 0; i < en.size(); i++) {
			nodes.get(i + 1).e = en.get(i);
		}
		nodes.get(0).e = new Enchantment("Item", 0).item();
		return t.getCompleteEnchantment();

	}

	public static Enchantment fillPermute(ETree t, List<Enchantment> en) {
		en.sort(Comparator.comparingInt(e -> -e.cost));
		t.invalidate();
		t.getMultiplier();
		List<ETree> nodes = t.getLeaves(new ArrayList<>());
		nodes.sort(Comparator.comparingInt(e -> e.multiplier));
		for (int i = 0; i < en.size(); i++) {
			nodes.get(i + 1).e = en.get(i);
		}
		nodes.get(0).e = new Enchantment("Item", 0).item();
		return t.getCompleteEnchantment();

	}

	public static List<List<Enchantment>> findPermutations(List<Enchantment> en, List<ETree> alignWith) {
		List<List<Enchantment>> permutes = new ArrayList<>();

		if (alignWith.size() == 0) {
			permutes.add(new ArrayList<>());
			return permutes;
		}
		ETree tree = alignWith.remove(0);
		int firstMultiplier = tree.multiplier;
		for (int i = 0; i < en.size(); i++) {
			if (i == 0 || firstMultiplier == alignWith.get(i - 1).multiplier) {
				Enchantment at = en.remove(i);
				List<List<Enchantment>> layer = findPermutations(en, alignWith);
				en.add(i, at);
				for (List<Enchantment> next : layer) {
					next.add(at);
					permutes.add(next);
				}
			}
		}
		alignWith.add(0, tree);

		return permutes;
	}

	public static List<ETree> getAllViableTrees(int size) {

		if (size <= 0 || (size & 1) == 0) return new ArrayList<>();

		List<ETree> res = new ArrayList<>();
		if (size == 1) {
			res.add(new ETree());
			return res;
		}

		for (int i = 1; i < size; i += 2) {
			if (i < size - i - 1) continue;//skip trees that can't work.

			List<ETree> leftSubTrees = getAllViableTrees(i);
			List<ETree> rightSubTrees = getAllViableTrees(size - i - 1);

			for (ETree l : leftSubTrees) {
				for (ETree r : rightSubTrees) {
					ETree root = new ETree();
					root.left = l;
					root.right = r;
					res.add(root);
				}
			}
		}

		return res;
	}

	public static List<Location> fill(int[] values, int pos, int value) {
		List<Location> loc = new ArrayList<>();
		values[pos] = value;
		if (left(pos) >= values.length) {
			loc.add(new Location(value, pos));
			return loc;
		}
		loc.addAll(fill(values, left(pos), value));
		loc.addAll(fill(values, right(pos), value + 1));
		return loc;
	}

	public static void fill(Enchantment[] values, int pos) {
		if (values[pos] != null) {
			return;
		}
		fill(values, left(pos));
		fill(values, right(pos));
		values[pos] = new Enchantment(values[left(pos)], values[right(pos)]);
	}

	private static int left(int index) {
		return 2 * index + 1;
	}

	private static int right(int index) {
		return 2 * index + 2;
	}

	public static Set<Enchantment> forceOptimize(List<Enchantment> en) {
		if (en.size() == 1) return new HashSet<>(en);

		Set<Enchantment> results = new HashSet<>();
		for (int i = 0; i < en.size(); i++) {
			for (int j = 0; j < en.size(); j++) {
				if (i == j) continue;
				if (en.get(j).isItem) continue;
				List<Enchantment> next = new ArrayList<>(en.size() - 1);
				next.add(new Enchantment(en.get(i), en.get(j)));
				for (int k = 0; k < en.size(); k++) {
					if (k == i) continue;
					if (k == j) continue;
					next.add(en.get(k));
				}
				Set<Enchantment> set = forceOptimize(next);
				if (!results.isEmpty() && set.iterator().next().weight < results.iterator().next().weight) {
					results.clear();
				} else if (!results.isEmpty() && set.iterator().next().weight > results.iterator().next().weight) {
					set.clear();
				}
				results.addAll(set);
			}
		}

		return results;
	}

	static record TreeOrder(ETree tree, List<Enchantment> ench) {
	}

	static class ETree {

		int multiplier = -1;
		Enchantment e;
		ETree left;
		ETree right;

		public ETree() {

		}

		public Enchantment getCompleteEnchantment() {
			if (left != null) {
				e = new Enchantment(left.getCompleteEnchantment(), right.getCompleteEnchantment());
				return e;
			}
			return e;
		}

		public int getMultiplier() {
			if (multiplier != -1) {
				return multiplier;
			}
			getMultiplier(0);
			return multiplier;
		}

		private int getMultiplier(int m) {
			multiplier = m;
			if (left != null) {
				right.getMultiplier(m + 1);
				left.getMultiplier(m);
			}
			return m;
		}

		//(((Item + Thorns) + (Sharpness + Mending)) + Respiration) 9, 502 135

		public int size() {
			int size = 1;
			if (left != null) {
				size += left.size();
				size += right.size();
			}
			return size;
		}

		public int depth() {
			int size = 1;
			if (left != null) {
				size += Math.max(left.size(), right.size());
			}
			return size;
		}

		public void invalidate() {
			multiplier = -1;
			e = null;
			if (left != null) {
				left.invalidate();
				right.invalidate();
			}
		}

		public String toString() {
			return multiplier + " x " + e;
		}

		public void print() {
			this.print("", true);
		}

		public void print(String indent, boolean last) {
			System.out.println(indent + "+- " + multiplier);
			indent += last ? "   " : "|  ";

			if (left != null) {
				right.print(indent, false);
				left.print(indent, true);
			}

		}

		public List<ETree> getNodes(List<ETree> tree) {
			if (left != null) {
				left.getNodes(tree);
				right.getNodes(tree);
			}
			tree.add(this);
			return tree;
		}

		public List<ETree> getLeaves(List<ETree> tree) {

			if (left != null) {
				left.getLeaves(tree);
				right.getLeaves(tree);
			} else {
				tree.add(this);
			}
			return tree;
		}
	}

	static class Location {

		int value;
		int position;

		public Location(int value, int position) {
			this.value = value;
			this.position = position;
		}

		public String toString() {
			return value + " at " + position;
		}
	}

	static class Enchantment {

		int cost;
		int tax;
		int weight;
		int combineCost;
		String name;
		boolean isItem = false;
		boolean useXp = false;

		public Enchantment(String name, int cost) {
			this.cost = cost;
			this.name = name;
		}

		public Enchantment(Enchantment a, Enchantment b) {
			name = "(" + a.name + " + " + b.name + ")";
			cost = a.cost + b.cost;
			tax = Math.max(a.tax, b.tax) * 2 + 1;
			combineCost = (b.cost + a.tax + b.tax);
			useXp = a.useXp || b.useXp;
			weight = (useXp ? levelsToXp(combineCost) : combineCost) + (a.weight + b.weight);

			isItem = a.isItem || b.isItem;
			if (!a.isItem && b.isItem) {
				new IllegalArgumentException("No items in right slot.").printStackTrace();
				;
			}
		}

		public Enchantment(Enchantment a) {
			name = a.name;
			cost = a.cost;
			tax = a.tax;
			weight = a.weight;
			isItem = a.isItem;
			combineCost = a.combineCost;
			useXp = a.useXp;
		}

		/*
		 * Total experience =
		level2 + 6 × level (at levels 0–16)
		2.5 × level2 – 40.5 × level + 360 (at levels 17–31)
		4.5 × level2 – 162.5 × level + 2220 (at levels 32+)
		 */
		public static int levelsToXp(int level) {
			if (level < 17) {
				return level * level + 6 * level;
			}
			if (level < 32) {
				return (int) (2.5 * level * level - 40.5 * level + 360);
			}
			return (int) (4.5 * level * level - 162.5 * level + 2220);
		}

		public Enchantment item() {
			isItem = true;
			return this;
		}

		public Enchantment useXp() {
			useXp = true;
			return this;
		}

		public String toString() {
			return name + (combineCost > 0 ? " " + combineCost + ", " + weight : "");
		}

		@Override
		public int hashCode() {
			return Objects.hash(name);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			Enchantment other = (Enchantment) obj;
			return Objects.equals(name, other.name);
		}

	}

	static class EnchantmentData {

		public static String[] names = { "Protection", "Fire Protection", "Feather Falling", "Blast Protection", "Projectile Protection", "Thorns",//
				"Respiration", "Depth Strider", "Aqua Affinity", "Sharpness", "Smite", "Bane of Arthropods", "Knockback", "Fire Aspect", "Looting",//
				"Efficiency", "Silk Touch", "Unbreaking", "Fortune", "Power", "Punch", "Flame", "Infinity", "Luck of the Sea", "Lure", "Frost Walker",//
				"Mending", "Curse of Binding", "Curse of Vanishing", "Impaling", "Riptide", "Loyalty", "Channeling", "Multishot", "Piercing", "Quick Charge",//
				"Soul Speed", "Swift Sneak", "Sweeping Edge", };
		public static int[] maxLevel = { 4, 4, 4, 4, 4, 3, 3, 3, 1, 5, 5, 5, 2, 2, 3, 5, 1, 3, 3, 5, 2, 1, 1, 3, 3, 2, 1, 1, 1, 5, 3, 3, 1, 1, 4, 3, 3, 3, 3, };
		public static int[] multiplier = { 1, 1, 1, 2, 1, 4, 2, 2, 2, 1, 1, 1, 1, 2, 2, 1, 4, 1, 2, 1, 2, 2, 4, 2, 2, 2, 2, 4, 4, 2, 2, 1, 4, 2, 1, 1, 4, 4, 2, };
	}
}
