
                if b_num-a_num==1 and self.tem_yixiang_index==0 and i<=1:  ##a的值肯定小于等于b的值， sum(a)>=number





            sum_dis = int(self.part_rate[self.roll] * min(sum(self.tem_seller['货物数量（张）']), sum(self.tem_buyer['购买货物数量'])))
            if rate>1:
                sum_dis=min(2*sum_dis,sum(self.tem_seller['货物数量（张）']), sum(self.tem_buyer['购买货物数量']))

            if self.tem_yixiang_index==0:
                self.tem_buyer =self.tem_buyer.sort_values(by=['平均持仓时间','权重'], ascending=self.flag2)
            else:
                self.tem_buyer =self.tem_buyer.sort_values(by=['权重'], ascending=self.flag2)
            if flag:
                tem_buyer1=tem_buyer1.sort_values(by=['权重'], ascending=self.flag2)
                self.tem_buyer=pd.concat([tem_buyer1,tem_buyer2])
            else:
                self.tem_buyer =self.tem_buyer.sort_values(by=['权重'], ascending=self.flag2) ##这里之后可以改下
                print('tt')
            buyer_tem_len = len(self.tem_buyer)


#part_rate=[0.4,0.6,,1]
part_rate=[0.4,1]
