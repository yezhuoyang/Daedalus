define i64 @main  {
main_entry_1:
    %a_1 = move 5
    %t_1 = move %a_1
    %a_1 = add %a_1 1
    %c_1 = move %t_1
    %b_1 = move %c_1
    %t_2 = add %b_1 %c_1
    %t_3 = add %t_2 %a_1
    %t_4 = move %t_3
    ret %t_4

}

define i64 @__init  {
__init_entry_1:
    %t_5 = call main 
    %t_6 = move %t_5
    ret %t_6

}

