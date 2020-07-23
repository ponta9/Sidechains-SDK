#!/usr/bin/env python2
from SidechainTestFramework.sc_test_framework import SidechainTestFramework
from test_framework.util import assert_true, assert_equal
from SidechainTestFramework.scutil import connect_sc_nodes, sc_p2p_port, initialize_default_sc_chain_clean, \
    start_sc_node, wait_for_next_sc_blocks, wait_for_sc_node_initialization, start_sc_nodes, generate_next_blocks
import sys
import json

"""
    Setup 3 SC Nodes and connect them togheter. Check that each node is connected to the other and that their initial keys/boxes/balances are
    coherent with the default initialization
"""

class SidechainNodesInitializationTest(SidechainTestFramework):

    def add_options(self, parser):
        #empty implementation
        pass
    
    def setup_chain(self):
        #empty implementation
        pass
        
    def setup_network(self, split = False):
        #empty implementation
        pass
    
    def sc_add_options(self, parser):

        lib_separator = ":"
        if sys.platform.startswith('win'):
            lib_separator = ";"

        binary = "../examples/carregistry/target/Sidechains-SDK-carregistry-0.2.0-SNAPSHOT.jar" + \
                 lib_separator + "../examples/carregistry/target/lib/* com.horizen.examples.CarRegistryApp"

        tmpdir = "../examples/carregistry/target/tmp"

        parser.remove_option("--scjarpath")
        parser.add_option("--scjarpath", dest="scjarpath", default=binary,
                          help="Directory containing .jar file for SC (default: %default)")
        parser.remove_option("--tmpdir")
        parser.add_option("--tmpdir", dest="tmpdir", default=tmpdir,
                          help="Root directory for datadirs")

    def sc_setup_chain(self):
        initialize_default_sc_chain_clean(self.options.tmpdir, 1)
        
    def sc_setup_network(self, split = False):
        self.sc_nodes = self.sc_setup_nodes()

    def sc_setup_nodes(self):
        return start_sc_nodes(1, dirname=self.options.tmpdir, binary=[self.options.scjarpath])
    
    def run_test(self):
        sc_node = self.sc_nodes[0]

        responce = sc_node.wallet_allBoxes()
        boxes = responce["result"]["boxes"]

        print("----------------------------------------------------------------------------------------------------")
        print("Boxes in wallet")
        print("----------------------------------------------------------------------------------------------------")
        print(json.dumps(boxes))
        print("----------------------------------------------------------------------------------------------------")

        assert_equal(1, len(boxes), "Unexpected number of boxes")

        public_key = boxes[0]["proposition"]["publicKey"]

        create_car_request = {"vin": "12345", "carProposition": public_key,
                              "year": 2010, "model": "Opel", "color": "Red", "description": "None", "fee": 20, "boxId": boxes[0]["id"]}

        print("----------------------------------------------------------------------------------------------------")
        print("Create car request")
        print("----------------------------------------------------------------------------------------------------")
        print(json.dumps(create_car_request))
        print("----------------------------------------------------------------------------------------------------")

        responce = sc_node.carApi_createCar(json.dumps(create_car_request))
        create_car_transaction = responce["result"]["createCarTxBytes"]

        send_transaction = {"transactionBytes": create_car_transaction}

        responce = sc_node.transaction_decodeTransactionBytes(json.dumps(send_transaction))
        transaction = responce["result"]

        print("----------------------------------------------------------------------------------------------------")
        print("Transaction of the car creation")
        print("----------------------------------------------------------------------------------------------------")
        print(json.dumps(transaction))
        print("----------------------------------------------------------------------------------------------------")

        responce = sc_node.transaction_sendTransaction(json.dumps(send_transaction))
        transaction_id = responce["result"]["transactionId"]

        responce = generate_next_blocks(sc_node, "Node 0", 1)

        responce = sc_node.wallet_allBoxes()
        boxes = responce["result"]["boxes"]

        print("----------------------------------------------------------------------------------------------------")
        print("Boxes in wallet")
        print("----------------------------------------------------------------------------------------------------")
        print(json.dumps(boxes))
        print("----------------------------------------------------------------------------------------------------")

        responce = sc_node.wallet_createPrivateKey25519()
        new_public_key = responce["result"]["proposition"]["publicKey"]

        print("----------------------------------------------------------------------------------------------------")
        print("New public key")
        print("----------------------------------------------------------------------------------------------------")
        print(new_public_key)
        print("----------------------------------------------------------------------------------------------------")

        send_coins_request = {
	        "outputs": [
		        {
			        "publicKey": new_public_key,
			        "value": 20000
		        }
	        ],
	        "fee": 10
        }

        responce = sc_node.transaction_sendCoinsToAddress(json.dumps(send_coins_request))
        transaction_id = responce["result"]["transactionId"]

        responce = generate_next_blocks(sc_node, "Node 0", 1)

        responce = sc_node.wallet_allBoxes()
        boxes = responce["result"]["boxes"]

        print("----------------------------------------------------------------------------------------------------")
        print("Boxes in wallet")
        print("----------------------------------------------------------------------------------------------------")
        print(json.dumps(boxes))
        print("----------------------------------------------------------------------------------------------------")

        box_of_car = filter(lambda b: b["typeId"] == 42, boxes)

        create_car_sell_order_request = {
	                "carBoxId": box_of_car[0]["id"],
	                "proposition": new_public_key,
	                "sellPrice": "10000"
                    }

        responce = sc_node.carApi_createCarSellOrder(json.dumps(create_car_sell_order_request))
        car_sell_order_transaction = responce["result"]["carSellOrderTxBytes"]

        send_transaction = {"transactionBytes": car_sell_order_transaction}

        responce = sc_node.transaction_decodeTransactionBytes(json.dumps(send_transaction))
        transaction = responce["result"]

        print("----------------------------------------------------------------------------------------------------")
        print("Transaction of the car sell order creation")
        print("----------------------------------------------------------------------------------------------------")
        print(json.dumps(transaction))
        print("----------------------------------------------------------------------------------------------------")

        responce = sc_node.transaction_sendTransaction(json.dumps(send_transaction))
        transaction_id = responce["result"]["transactionId"]

        responce = generate_next_blocks(sc_node, "Node 0", 1)

        responce = sc_node.wallet_allBoxes()
        boxes = responce["result"]["boxes"]

        print("----------------------------------------------------------------------------------------------------")
        print("Boxes in wallet")
        print("----------------------------------------------------------------------------------------------------")
        print(json.dumps(boxes))
        print("----------------------------------------------------------------------------------------------------")

        box_to_pay_for_car = filter(lambda b: b["proposition"]["publicKey"] == new_public_key, boxes)
        car_sell_order_box = filter(lambda b: b["typeId"] == 43, boxes)

        accept_car_sell_order_request = {
	        "carSellOrderId": car_sell_order_box[0]["id"],
	        "paymentRegularBoxId": box_to_pay_for_car[0]["id"],
	        "buyerProposition": new_public_key
        }

        responce = sc_node.carApi_acceptCarSellOrder(json.dumps(accept_car_sell_order_request))
        accept_car_sell_order_transaction = responce["result"]["acceptedCarSellOrderTxBytes"]

        send_transaction = {"transactionBytes": accept_car_sell_order_transaction}

        responce = sc_node.transaction_decodeTransactionBytes(json.dumps(send_transaction))
        transaction = responce["result"]

        print("----------------------------------------------------------------------------------------------------")
        print("Transaction of the car sell order acception")
        print("----------------------------------------------------------------------------------------------------")
        print(json.dumps(transaction))
        print("----------------------------------------------------------------------------------------------------")

        responce = sc_node.transaction_sendTransaction(json.dumps(send_transaction))
        transaction_id = responce["result"]["transactionId"]

        responce = generate_next_blocks(sc_node, "Node 0", 1)

        responce = sc_node.wallet_allBoxes()
        boxes = responce["result"]["boxes"]

        print("----------------------------------------------------------------------------------------------------")
        print("Boxes in wallet")
        print("----------------------------------------------------------------------------------------------------")
        print(json.dumps(boxes))
        print("----------------------------------------------------------------------------------------------------")

if __name__ == "__main__":
    SidechainNodesInitializationTest().main()