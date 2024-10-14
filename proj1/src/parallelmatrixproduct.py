import time


def OnMult(m_ar, m_br):

    pha = [1.0] * (m_ar * m_ar)
    phb = [0] * (m_ar * m_ar)
    phc = [0] * (m_ar * m_ar)
    
    for i in range(m_br):
        for j in range(m_br):
            phb[i*m_br +j] = i + 1


    start_time = time.time()

    for i in range(m_ar):
        for j in range(m_br):
            temp = 0
            for k in range(m_ar):
                temp += pha[i * m_ar + k] * phb[k * m_br + j]
            phc[i * m_ar + j] = temp

    end_time = time.time()
    execution_time = end_time - start_time

    print("Time:", "{:.3f}".format(execution_time), "seconds")

    # display 10 elements of the result matrix to verify correctness
    print("Result matrix:")
    for i in range(1):
        for j in range(min(10, m_br)):
            print(phc[j], end=" ")
        print()    

def OnMultLine(m_ar,m_br):

    pha = [1.0] * (m_ar * m_ar)
    phb = [0] * (m_ar * m_ar)
    phc = [0] * (m_ar * m_ar)
    
    for i in range(m_br):
        for j in range(m_br):
            phb[i*m_br +j] = i + 1

    start_time = time.time()

    # perform element-wise multiplication
    for i in range(m_ar):
        for k in range(m_ar):
            for j in range(m_br):
                phc[i * m_br + j] += pha[i * m_ar + k] *  phb[k * m_br + j]

    end_time = time.time()
    execution_time = end_time - start_time

    print("Time:", "{:.3f}".format(execution_time), "seconds")

    # display 10 elements of the result matrix to verify correctness
    print("Result matrix:")
    for i in range(1):
        for j in range(min(10, m_ar)):
            print(phc[j], end=" ")
        print()

def OnMultBlock(m_ar,m_br,bkSize):

    pha = [1.0] * (m_ar * m_ar)
    phb = [0] * (m_ar * m_ar)
    phc = [0] * (m_ar * m_ar)
    
    for i in range(m_br):
        for j in range(m_br):
            phb[i*m_br +j] = i + 1

    start_time = time.time()

    # perform block-wise multiplication

    for ii in range(0,m_ar,bkSize):
        for jj in range(0,m_br,bkSize):
            for kk in range(0,m_ar,bkSize):
                for i in range(ii,min(ii+bkSize,m_ar)):
                    for k in range(kk,min(kk+bkSize,m_ar)):
                        for j in range(jj,min(jj+bkSize,m_br)):
                            phc[i * m_br + j] += pha[i * m_ar + k] *  phb[k * m_br + j]

    end_time = time.time()
    execution_time = end_time - start_time

    print("Time:", "{:.3f}".format(execution_time), "seconds")

    # display 10 elements of the result matrix to verify correctness
    print("Result matrix:")
    for i in range(1):
        for j in range(min(10, m_ar)):
            print(phc[j], end=" ")
        print()


def main():
    print( "1. Multiplication\n")
    print( "2. Line Multiplication\n")
    print( "3. Block Multiplication\n")
    selection = int(input("Selection?:"))
    dimensions = int(input("Dimensions: lins=cols ? "))
    if (selection == 1):
        OnMult(dimensions, dimensions)
    elif (selection == 2):
        OnMultLine(dimensions, dimensions)
    elif (selection == 3):
        print( "Chose a block size:")
        blkSize = int(input("Input?:"))
        OnMultBlock(dimensions, dimensions,blkSize)

if __name__ == "__main__":
    main()