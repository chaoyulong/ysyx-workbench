#ifndef __simulation_h__
#define __simulation_h__

#define STR_HELPER(x) #x
#define STR(x) STR_HELPER(x)


void single_cycle();
void n_cycle(int n);
void reset(int n);
void sim_init(int argc, char *argv[]);
void sim_exit();

#endif

