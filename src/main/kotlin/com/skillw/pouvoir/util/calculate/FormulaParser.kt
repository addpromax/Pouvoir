package com.skillw.pouvoir.util.calculate

import com.skillw.pouvoir.util.calculate.CalcOperator.Companion.isCalcOperator
import com.skillw.pouvoir.util.calculate.CalcOperator.Companion.toCalcOperator
import taboolib.common.platform.function.warning
import java.util.*

private fun String.toCalcInfix(): ArrayList<Any> {
    //将数字与运算符分开，按中缀表达式顺序排列在List中
    val infix = ArrayList<Any>()
    with(filter { it != ' ' }) {
        var begin = 0
        forEachIndexed loop@{ index, char ->
            if (!char.isCalcOperator()) return@loop
            val num = substring(begin, index)
            begin = index + 1
            if (num.isNotEmpty()) {
                infix += num.toDouble()
            }
            val operator = char.toCalcOperator()
            if (infix.lastOrNull() !is Double && operator == CalcOperator.MINUS){
                infix += 0.0
            }
            infix += operator
        }
        substring(begin).toDoubleOrNull()?.let {
            infix += it
        }
    }
    return infix
}

private fun Stack<CalcOperator>.nextNotLessThan(calcOperator: CalcOperator): Boolean {
    return isNotEmpty() && peek() != CalcOperator.LEFT_BRACKET && peek().priority >= calcOperator.priority
}

/**
 * To suffix
 *
 * @return
 */
private fun List<Any>.toCalcSuffix(): Queue<Any> {
    val suffix = ArrayDeque<Any>()
    val operators = Stack<CalcOperator>()
    forEach {
        if (it is Double) {
            suffix.offerLast(it)
            return@forEach
        }
        with(operators) {
            when (val calcOperator = it as CalcOperator) {
                CalcOperator.LEFT_BRACKET -> push(calcOperator)
                CalcOperator.RIGHT_BRACKET -> {
                    while (isNotEmpty() && peek() != CalcOperator.LEFT_BRACKET) {
                        suffix.offerLast(pop())
                    }
                    if (isNotEmpty())
                        pop()
                }

                else -> {
                    while (nextNotLessThan(calcOperator)) {
                        suffix.offerLast(pop())
                    }
                    push(calcOperator)
                }
            }
        }
    }
    while (!operators.isEmpty()) {
        suffix.offerLast(operators.pop())
    }
    return suffix
}

private fun Queue<Any>.calc(): Double {
    val calcStack: Stack<Double> = Stack()
    while (isNotEmpty()) {
        val obj = poll()
        if (obj is Double) {
            calcStack.push(obj)
            continue
        }
        val calcOperator = obj as CalcOperator
        val a = if (calcStack.isEmpty()) 0.0 else calcStack.pop()
        val b = if (calcStack.isEmpty()) 0.0 else calcStack.pop()
        calcStack.push(calcOperator.calc(a, b))
    }
    return calcStack.pop()
}

internal fun String.calculateInfix(): Double {
    return runCatching {
        filter { it != ' ' }.toCalcInfix().toCalcSuffix().calc()
    }.getOrElse {
        warning("Wrong calculation formula! $this");
        0.0
    }
}