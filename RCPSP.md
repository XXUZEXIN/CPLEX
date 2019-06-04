% CPLEX  matlab上实现
% a cplex code of RCPSP on matlab
function solution=cplex_function()
% 定义的全部变量，储存项目信息
global Case 
% 假设的时间最大值
H=sum(Case.dur);
% 定义变量   bin是0 1 值  full代表是满的矩阵，非稀疏矩阵
x=binvar(Case.job_count,H,'full');
% 资源上限，是常数
r=Case.Con_R;
% 定义中间变量   int 是整数值，可以是任意整数
C=intvar(1);
% 目标函数
Objective=min(C); 

% 约束
Constraints=[];
% 作业时间约束
% dur 代表作业持续时间
x2=sum(x,2);
for i=1:Case.job_count    
    Constraints=[Constraints,x2(i)==Case.dur(i)];
end
for j=1:Case.job_count 
    for i=1:H
        Constraints=[Constraints,x(j,i)*i<=C];
    end
end
    
% prec代表作业间约束 
% 紧前紧后约束
for j=2:Case.job_count
    Pjj=[];
    for jj=1:j
        if(jj~=j && Case.prec(jj,j)==1)
            Pjj=[Pjj jj];
        end
    end
    for index=1:length(Pjj) %对于任意一个j的紧前任务
        for d=1:H
            last=0;
            for dd=1:d-1
                last=last+x(Pjj(index),dd);
            end
            Constraints=[Constraints,Case.dur(Pjj(index))*x(j,d)<=last];
        end
    end
end


% 作业工期约束
% for j=1:Case.job_count
%     for d=1:Cases.Times
%          Constraints=[Constraints,d*x(j,d)<=Paras.T]; 
%     end
% end

% 作业不可中断
for j=1:Case.job_count
    for d=1:H-1
        dur_instant=0;
        for q=d+2:H
            dur_instant=dur_instant+x(j,q);
        end
        Constraints=[Constraints,Case.dur(j)*x(j,d)-Case.dur(j)*x(j,d+1)+ dur_instant<=Case.dur(j)];
    end
end
% 资源约束
% r代表给定资源数
for k=1:4
    for d=1:H
        total_alloc=0;
        for j=1:Case.job_count
            total_alloc=total_alloc+x(j,d)*Case.res(j,k);
        end
         Constraints=[Constraints,total_alloc<=r(k)];
    end
end

Constraints=[Constraints,0<C<H];    %目标函数C的约束


% 约束构建好了接下来是求解过程，所有的都一样
options = sdpsettings('solver','cplex','showprogress',1,'verbose',2);
% 这是最大求解时间
options.cplex.MaxTime=36000;  
options.cplex.NodeDisplayInterval=1;
sol=optimize(Constraints,Objective,options);

if strcmp(sol.info,'Successfully solved (CPLEX-IBM)')==true
    solution.Objective=value(Objective);
    display(solution.Objective);
    solution.feasibility='feasible'
    solution.info=sol.info;
    solution.x=value(C);     
    for j=1:Case.job_count   
         for t=1:H
             if(double(x(j,t))==1)
                 solution.st(j)=t;
                 break;
             end
         end
     end
             
elseif strcmp(sol.info,'Maximum iterations or time limit exceeded (CPLEX-IBM)')==true
    solution.Objective=value(Objective);
    display(solution.Objective);
    solution.feasibility='unknown';
    solution.info=sol.info;
else
    solution.feasibility='infeasible';
    solution.solvertime=sol.solvertime;
    solution.x=value(C);
end
