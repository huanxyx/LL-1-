package com;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class LL1 {
	private List<PRO> listOfPRO;							//产生式的集合
	private Set<Character> setOfNullable;					//能推导出ε的非终结符
	private Map<Character, Set<Character>> mapOfFirst;		//每个非终结符的First集合
	private Map<Character, Set<Character>> mapOfFollow;		//每个非终结符的Follow集合
	private List<Set<Character>> listOfFirst_S; 			//计算每条产生式的First_S集合
	private boolean isCFG = false;							//标志是否是上下文无关文法
	
	/*
	 * 根据一个输入流初始化产生式，并进行一定的运算得出每一个产生式的FIRST_S集合
	 */
	public LL1(InputStream is){
		//1.翻译上下文无关文法文件
		translateFile(is);
		
		//2.根据该文法求出NULLABLE集合
		calculateNullableSet();
		
		//3.根据NULLABLE集合以及该文法求出每一个非终结符的FIRST集合
		calculateFirstMap();
		
		//4.根据NULLABEL集合以及FIRST集合求出每一个非终结符的FOLLOW集合
		calculateFollowMap();
		
		//5.最后求出每个产生式的FIRST_S集合
		calculateFirst_SList();
		
		//6.判断产生式左部的FIRST_S集合是否有交集，若是有交集则不为LL1文法
		judggCFG();
	}
	
	/**
	 * 将存储文法的文件输入流转换为一个产生式的集合
	 * @param is
	 */
	private void translateFile(InputStream is) {
		listOfPRO = new ArrayList<PRO>();
		
		//扫描输入文件,转变成一个产生式的集合
		Scanner sc = new Scanner(is);
		while(sc.hasNext()) {
			String line = sc.nextLine();
			if( !line.equals("!") ) {
				char left = line.charAt(0);			
				String right = line.substring(3);
				
				//获取产生式
				PRO pro = new PRO();
				pro.setLeft(left);
				pro.setRight(right);
				
				listOfPRO.add(pro);
			}
		}
		
		sc.close();
	}
	
	/**
	 * 计算Nullable集合:所有能够推导出ε的非终结符
	 */
	private void calculateNullableSet() {
		setOfNullable = new HashSet<Character>();
		boolean hasChange = true; 					//代表着集合是否发生了改变
		
		while( hasChange ) {
			hasChange = false;
			
			//遍历所有的产生式
			for(PRO pro : listOfPRO) {
				char left = pro.getLeft();
				String right = pro.getRight();
				
				if( right.length() == 1 && right.charAt(0) == 'ε' ) {
					//判断能否直接推出ε
					if( !setOfNullable.contains(left) ) {
						setOfNullable.add(left);
						hasChange = true;
					}
				} else if( LL1Utils.isBelongToNull(right, setOfNullable) ) {
					//判断能否间接推出ε
					if( !setOfNullable.contains(left) ) {
						setOfNullable.add(left);
						hasChange = true;
					}
				}
			}
		}
	}
	
	/**
	 * 用于计算每个非终结符的First集合
	 */
	private void calculateFirstMap() {
		mapOfFirst = new HashMap< Character, Set<Character> >();
		
		//初始化所有非终结符的FIRST集合
		initSet(mapOfFirst);
		
		boolean hasChange = true;
		
		while( hasChange ) {								//当Map不再发生变化
			hasChange = false;
			
			//遍历每一条产生式
			for( PRO pro : listOfPRO ) {
				char left = pro.getLeft();
				String right = pro.getRight();
				char[] rightBuffer = right.toCharArray();
				Set<Character> set = mapOfFirst.get(left);				//代表着每一个该产生式右部的非终结符的FIRST集
				
				//遍历产生式右部每一个字符
				for( char ch : rightBuffer ) {
					if( LL1Utils.isFinalChar(ch) && ch != 'ε') {
						//是一个终结符的时候
						if( !set.contains(ch) ) {
							//不包含该字符的时候
							set.add(ch);
							hasChange = true;
						}
						break;							//不用继续遍历了，因为该位置为终结符，不能再看其后的字符了
					} else if( !LL1Utils.isFinalChar(ch) ){
						//不是一个终结符的时候
						
						Set<Character> tempSet = mapOfFirst.get(ch);		//获取该非终结符的FIRST集合
						//有可添加的非终结符
						if( !set.containsAll(tempSet) ) {
							set.addAll(tempSet);
							hasChange = true;
						}
						//判断该非终结符是否能推导出ε，能的话继续看下一个字符,不能的话则终止
						if( !LL1Utils.isBelongToNull( ch, setOfNullable) ) {
							break;
						}
					}
				}
			}
		}
		
	}
	
	/**
	 * 用于计算每个非终结符的Follow集合
	 */
	private void calculateFollowMap() {
		mapOfFollow = new HashMap< Character, Set<Character> >();
		
		//初始化每个非终结符的Follow集
		initSet(mapOfFollow);
		
		boolean hasChange = true;
		
		while( hasChange ) {
			hasChange = false;
			
			//遍历每一条产生式
			for( PRO pro : listOfPRO ) {
				char left = pro.getLeft();
				String right = pro.getRight();
				char[] rightBuffer = right.toCharArray();
				Set<Character> set = mapOfFollow.get(left);				
				Set<Character> tempSet = new HashSet<Character>();		//用来存储当前遍历的产生式右部的非终结符的Follow集合。
				tempSet.addAll(set);				//将该产生式左部的follow集添加到其中
						
				//逆序遍历产生式右部每一个字符
				for(int i = rightBuffer.length - 1; i >= 0; i--) {
					char tempChar = rightBuffer[i];
								
					if( LL1Utils.isFinalChar(tempChar) && tempChar != 'ε') {
						//为终结符
						tempSet.clear();
						tempSet.add(tempChar);
					} else if( !LL1Utils.isFinalChar(tempChar) ) {
						//为非终结符
						
						//1.获取该非终结符的Follow集
						Set<Character> noterminalSet = mapOfFollow.get( tempChar );
						//2.将tempSet添加到该非终结符的Follow集合中
						if( !noterminalSet.containsAll( tempSet ) ) {
							//不完全包含才添加
							noterminalSet.addAll(tempSet);
							hasChange = true;
						}
						//3.该非终结符不能推出ε的话，则清空tempSet集合
						if( !LL1Utils.isBelongToNull(tempChar, setOfNullable) ) {
							tempSet.clear();
						} 
						//4.合并当前非终结符的FIRST集
						tempSet.addAll( mapOfFirst.get(tempChar) );
					}
				}
			}
			
		}
	}
	
	/**
	 * 用于计算每一条产生式的FIRST_S集合
	 */
	private void calculateFirst_SList() {
		listOfFirst_S = new ArrayList<Set<Character>>();
		
		//1.初始化
		initSet(listOfFirst_S);
		
		//2.遍历每一条产生式，求出他的FIRST_S集合
		for(int i = 0; i < listOfPRO.size(); i++) {
			calculateFirst_S(i);
		}
	}
	
	/**
	 * 求出每一条产生式的FIRST_S集合
	 */
	private void calculateFirst_S(int i) {
		PRO pro = listOfPRO.get(i);
		char left = pro.getLeft();
		String right = pro.getRight();
		char[] rightBuffer = right.toCharArray();
		Set<Character> first_s = listOfFirst_S.get(i);			//该产生式的FIRST_S
		
		for( char ch : rightBuffer ) {
			if( LL1Utils.isFinalChar(ch) && ch != 'ε' ) {
				//为终结符的时候
				first_s.add(ch);
				return ;
			} else if( !LL1Utils.isFinalChar(ch) ) {
				//为非终结符的时候
				first_s.addAll( mapOfFirst.get(ch) );
				
				if( !LL1Utils.isBelongToNull(ch, setOfNullable) ) {
					return ;
				}
			}
		}
		
		//假如产生式右部能够推出ε，则将该左部的非终结符的follow添加到其中。
		first_s.addAll( mapOfFollow.get(left) );
	}
	
	/**
	 * 判断该文法是否是上下文无关文法
	 */
	private void judggCFG() {
		int size = listOfFirst_S.size();
		for(int i = 0; i < size-1; i++) {
			PRO pro1 = listOfPRO.get(i);
			Set<Character> set1 = listOfFirst_S.get(i);
			for(int j = i+1; j < size; j++) {
				PRO pro2 = listOfPRO.get(j);
				
				//两个产生式左部相同
				if( pro1.getLeft() == pro2.getLeft() ) {
					Set<Character> set2 = listOfFirst_S.get(j);
					for(char ch : set2) {
						//两者存在交集则不是CFG
						if( set1.contains(ch) ) {
							isCFG = false;
							return ;
						}
					}
				}
			}
		}
		isCFG = true;
	}
	
	
	/**
	 * 初始化集合
	 */
	private void initSet(Map<Character, Set<Character>> map) {
		for( PRO pro : listOfPRO ) {
			char left = pro.getLeft();
			
			if ( !map.containsKey(left) ) {
				Set<Character> set = new HashSet<Character>();
				map.put(left, set);
			}
		}
	}
	
	private void initSet(List<Set<Character>> list) {
		for( PRO pro : listOfPRO ) {			
			Set<Character> set = new HashSet<Character>();
			list.add(set);
		}
	}
	
	
	//以下方法用于测试
	public void printPRO(){
		for(PRO pro : listOfPRO) {
			System.out.println(pro.getLeft() + "->" + pro.getRight());
		}
	}
	
	public void printNull(){
		for(char ch : setOfNullable) {
			System.out.println(ch + ",");
		}
	}
	
	public void printFirst(){
		for(Map.Entry<Character, Set<Character>> entry : mapOfFirst.entrySet()) {
			char nFinalChar = entry.getKey();
			Set<Character> fisrtSet = entry.getValue();
			
			System.out.print(nFinalChar + ":");
			for( char ch : fisrtSet ) {
				System.out.print(ch + ",");
			}
			System.out.println();
		}
	}
	
	public void printFollow(){
		for(Map.Entry<Character, Set<Character>> entry : mapOfFollow.entrySet()) {
			char nFinalChar = entry.getKey();
			Set<Character> followSet = entry.getValue();
			
			System.out.print(nFinalChar + ":");
			for( char ch : followSet ) {
				System.out.print(ch + ",");
			}
			System.out.println();
		}
	}
	
	public void printFirst_S(){
		for(int i = 0; i < listOfFirst_S.size(); i++) {
			Set<Character> set = listOfFirst_S.get(i);
			System.out.print(i + ":");
			for(char ch : set) {
				System.out.print( ch + ",");
			}
			System.out.println();
		}
	}
	
	public boolean isCFG() {
		return isCFG;
	}
	
	/*
	 * 单元测试
	 */
	public static void main(String[] args) throws FileNotFoundException {
		
		//1.输入一个文法（大写字母为非终结符，非大写字母为终结符）
		InputStream is = new FileInputStream( new File("input.data") );
		
		//2.将该文本传入的文法转换为一个LL1对象
		LL1 ll1 = new LL1(is);
		
		//3.在该LL1对象上进行操作
//		ll1.printPRO();
//		ll1.printNull();
//		ll1.printFirst();
//		ll1.printFollow();
//		ll1.printFirst_S();
		System.out.println(ll1.isCFG());
	}
	
	//产生式的类
	public class PRO {
		private char left;			//该产生式的左部
		private String right;		//该产生式的右部
		public char getLeft() {
			return left;
		}
		public void setLeft(char left) {
			this.left = left;
		}
		public String getRight() {
			return right;
		}
		public void setRight(String right) {
			this.right = right;
		}
	}
}
