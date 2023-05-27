# Compilers Project 2022/2023

Full Java-- grammar support with all requested optimizations.

## Optimizations

- Constant Folding
- Constant Propagation
- Register Allocation

## How to run

Flags available:

- `-o` - Constant Folding and Propagation Optimizations
- `-d` - Debug (shows extra information)
- `-r=<NUM>` - Number of registers to use 

The number of defaults registers is `-1`, which means that the compiler will use the number of registers from OLLIR.
By using `0`, the compiler will try to use the least number of registers.
Otherwise, the compiler will use the number of registers specified and give error if it is not possible.

## Authors

- Lia Vieira <up202005042@up.pt> (Contribution: 33.3%)
- Marco André <up202004891@up.pt> (Contribution: 33.3%)
- Ricardo Matos <up202007962@up.pt> (Contribution: 33.3%)

**Self-evaluation**: 20 / 20
