package com.indianservers.aiexplorer.mathworkspace.discrete

import java.math.BigInteger
object CombinatoricsEngine {
    fun factorial(n:Int):BigInteger { require(n in 0..10_000){"n must be between 0 and 10,000"};return (2..n).fold(BigInteger.ONE){a,b->a*BigInteger.valueOf(b.toLong())} }
    fun choose(n:Int,r:Int):BigInteger { require(n in 0..10_000&&r>=0&&r<=n){"Use 0 ≤ r ≤ n ≤ 10,000"};val k=minOf(r,n-r);var x=BigInteger.ONE;for(i in 1..k)x=x*BigInteger.valueOf((n-k+i).toLong())/BigInteger.valueOf(i.toLong());return x }
    fun permute(n:Int,r:Int):BigInteger { require(n in 0..10_000&&r>=0&&r<=n){"Use 0 ≤ r ≤ n ≤ 10,000"};return (n-r+1..n).fold(BigInteger.ONE){a,b->a*BigInteger.valueOf(b.toLong())} }
    fun pascal(row:Int)= (0..row).map{choose(row,it)}
    fun combinations(items:List<String>,r:Int,limit:Int=100):List<List<String>> {val out=mutableListOf<List<String>>();fun walk(i:Int,left:Int,chosen:MutableList<String>){if(out.size>=limit)return;if(left==0){out+=chosen.toList();return};for(j in i..items.size-left){chosen+=items[j];walk(j+1,left-1,chosen);chosen.removeAt(chosen.lastIndex)}};if(r in 0..items.size)walk(0,r,mutableListOf());return out}
}
